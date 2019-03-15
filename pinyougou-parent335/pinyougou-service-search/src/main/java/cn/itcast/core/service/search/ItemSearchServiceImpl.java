package cn.itcast.core.service.search;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import javax.annotation.Resource;
import java.util.*;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Resource
    private SolrTemplate solrTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ItemDao itemDao;

    /**
     * 前台系统的检索
     * @param searchMap
     * @return
     */
    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {
        // 封装所有的结果集
        Map<String, Object> resultMap = new HashMap<>();
        // 去除关键字中包含的空格问题
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            // trim：去前后的空格。angularjs中：对检索条件前后的空格自动去除
            keywords = keywords.replace(" ", "");
            searchMap.put("keywords", keywords);
        }
        // 1、根据关键字检索并且结果分页
//        Map<String, Object> map = searchForPage(searchMap);
//        resultMap.putAll(map);
        Map<String, Object> map = searchForHighLightPage(searchMap);
        resultMap.putAll(map);
        // 2、加载商品分类
        List<String> categoryList = searchForGroupPage(searchMap);
        resultMap.put("categoryList", categoryList);
        // 3、默认加载第一个分类下的商品品牌以及规格
        if(categoryList != null && categoryList.size() > 0){
            Map<String, Object> brandAndSpecMap = defaultSelectBrandAndSpecByCategoryByName(categoryList.get(0));
            resultMap.putAll(brandAndSpecMap);
        }
        return resultMap;
    }

    @Override
    public void isShow(Long id) {
        // 将该商品对应的库存中价格最低的sku保存到索引库中
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id).andStatusEqualTo("1")
                .andIsDefaultEqualTo("1").andNumGreaterThan(0);
        List<Item> items = itemDao.selectByExample(itemQuery);
        if(items != null && items.size() > 0){
            // 处理商品的规格
            for (Item item : items) {
                String spec = item.getSpec();
                Map<String, String> specMap = JSON.parseObject(spec, Map.class);
                item.setSpecMap(specMap);
            }
            // 将数据保存到索引库中
            solrTemplate.saveBeans(items);
            solrTemplate.commit();
        }
    }

    /**
     * 商品下架
     * @param id
     */
    @Override
    public void deleteItemFromSolr(Long id) {
        SimpleQuery query = new SimpleQuery("item_goodsid:"+id);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }

    // 默认加载第一个分类下的商品品牌以及规格
    private Map<String,Object> defaultSelectBrandAndSpecByCategoryByName(String categoryName) {
        // 通过分类获取模板id
        Object typeId = redisTemplate.boundHashOps("itemCat").get(categoryName);
        // 通过模板id取出品牌结果集
        List<Map> brandList = (List<Map>) redisTemplate.boundHashOps("brandList").get(typeId);
        // 通过模板id取出规格结果集
        List<Map> specList = (List<Map>) redisTemplate.boundHashOps("specList").get(typeId);

        // 封装数据
        Map<String,Object> map = new HashMap<>();
        map.put("brandList", brandList);
        map.put("specList", specList);
        return map;
    }

    // 加载商品的分类
    private List<String> searchForGroupPage(Map<String, String> searchMap) {
        // 1、封装检索条件
        Criteria criteria = new Criteria("item_keywords"); // 指定检索的字段
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            criteria.is(keywords);  // 会切词，根据词条检索
        }
        SimpleQuery query = new SimpleQuery(criteria);
        // 2、设置分组条件
        GroupOptions groupOptions = new GroupOptions();
        groupOptions.addGroupByField("item_category");
        query.setGroupOptions(groupOptions);

        List<String> list = new ArrayList<>();
        // 3、根据条件查询
        GroupPage<Item> groupPage = solrTemplate.queryForGroupPage(query, Item.class);
        GroupResult<Item> groupResult = groupPage.getGroupResult("item_category");
        Page<GroupEntry<Item>> groupEntries = groupResult.getGroupEntries();
        for (GroupEntry<Item> groupEntry : groupEntries) {
            String groupValue = groupEntry.getGroupValue();
            list.add(groupValue);
        }
        // 4、封装结果
        return list;
    }

    // 根据关键字检索并且结果分页以外：关键字高亮显示
    private Map<String,Object> searchForHighLightPage(Map<String, String> searchMap) {
        // 1、封装检索条件
        Criteria criteria = new Criteria("item_keywords"); // 指定检索的字段
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            criteria.is(keywords);  // 会切词，根据词条检索
        }
        SimpleHighlightQuery query = new SimpleHighlightQuery(criteria);
        // 2、封装分页条件
        Integer pageNo = Integer.valueOf(searchMap.get("pageNo"));
        Integer pageSize = Integer.valueOf(searchMap.get("pageSize"));
        Integer startRow = (pageNo - 1) * pageSize;
        query.setOffset(startRow);    // 其始行
        query.setRows(pageSize);      // 每页显示的条数
        // 3、封装高亮条件
        HighlightOptions highlightOptions = new HighlightOptions();
        highlightOptions.addField("item_title");    // 如果标题中有关键字需要高亮显示
        highlightOptions.setSimplePrefix("<font color='red'>"); // 开始标签
        highlightOptions.setSimplePostfix("</font>");
        query.setHighlightOptions(highlightOptions);
        // select * form table where keywords like ? and filed1 like ? and filed2 like ?
        // 继续封装：过滤条件：分类、品牌、规格、价格
        // 分类
        String category = searchMap.get("category");
        if(category != null && !"".equals(category)){
            Criteria cri = new Criteria("item_category");
            cri.is(category);
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);
        }
        // 品牌
        String brand = searchMap.get("brand");
        if(brand != null && !"".equals(brand)){
            Criteria cri = new Criteria("item_brand");
            cri.is(brand);
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);
        }
        // 规格
        String spec = searchMap.get("spec");
        if(spec != null && !"".equals(spec)){
            // 索引库中的规格字段：动态的字段：item_spec_*
            // spec数据：{内存：16G，网络：4G}
            Map<String, String> map = JSON.parseObject(spec, Map.class);
            Set<Map.Entry<String, String>> entries = map.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                Criteria cri = new Criteria("item_spec_"+entry.getKey());
                cri.is(entry.getValue());
                SimpleFilterQuery filterQuery = new SimpleFilterQuery(cri);
                query.addFilterQuery(filterQuery);
            }
        }

        // 价格
        String price = searchMap.get("price");
        if(price != null && !"".equals(price)){
            // 价格条件：区间段 [min - max]   between
            String[] prices = price.split("-");
            Criteria cri = new Criteria("item_price");
            if(price.contains("*")){ // xxx以上
                cri.greaterThanEqual(prices[0]);
            }else{ // min-max
                cri.between(prices[0], prices[1], true, true);
            }
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(cri);
            query.addFilterQuery(filterQuery);
        }

        // 根据新品、价格排序
        // sortField：排序的字段 sort：排序规格（ASC/DESC）
        String sort = searchMap.get("sort");
        if(sort != null && !"".equals(sort)){
            if("ASC".equals(sort)){
                Sort s = new Sort(Sort.Direction.ASC, "item_" + searchMap.get("sortField"));
                query.addSort(s);
            }else{
                Sort s = new Sort(Sort.Direction.DESC, "item_" + searchMap.get("sortField"));
                query.addSort(s);
            }
        }

        // 4、根据条件查询
        HighlightPage<Item> highlightPage = solrTemplate.queryForHighlightPage(query, Item.class);
        // 处理高亮结果
        List<HighlightEntry<Item>> highlighted = highlightPage.getHighlighted();
        if(highlighted != null && highlighted.size() > 0){
            for (HighlightEntry<Item> highlightEntry : highlighted) {
                Item item = highlightEntry.getEntity(); // 普通的标题
                List<HighlightEntry.Highlight> highlights = highlightEntry.getHighlights(); // 高亮的标题
                if(highlights != null && highlights.size() > 0){
                    String title = highlights.get(0).getSnipplets().get(0);
                    item.setTitle(title);
                }
            }
        }
        // 5、封装结果
        Map<String,Object> map = new HashMap<>();
        map.put("totalPages", highlightPage.getTotalPages());  // 总页数
        map.put("total", highlightPage.getTotalElements());    // 总条数
        map.put("rows", highlightPage.getContent());           // 商品列表结果集
        return map;
    }

    // 根据关键字检索并且结果分页
    private Map<String,Object> searchForPage(Map<String, String> searchMap) {
        // 1、封装检索条件
        Criteria criteria = new Criteria("item_keywords"); // 指定检索的字段
        String keywords = searchMap.get("keywords");
        if(keywords != null && !"".equals(keywords)){
            criteria.is(keywords);  // 会切词，根据词条检索
        }
        SimpleQuery simpleQuery = new SimpleQuery(criteria);
        // 2、封装分页条件
        Integer pageNo = Integer.valueOf(searchMap.get("pageNo"));
        Integer pageSize = Integer.valueOf(searchMap.get("pageSize"));
        Integer startRow = (pageNo - 1) * pageSize;
        simpleQuery.setOffset(startRow);    // 其始行
        simpleQuery.setRows(pageSize);      // 每页显示的条数
        // 3、根据条件查询
        ScoredPage<Item> scoredPage = solrTemplate.queryForPage(simpleQuery, Item.class);
        // 4、封装结果
        Map<String,Object> map = new HashMap<>();
        map.put("totalPages", scoredPage.getTotalPages());  // 总页数
        map.put("total", scoredPage.getTotalElements());    // 总条数
        map.put("rows", scoredPage.getContent());           // 商品列表结果集
        return map;
    }
}
