package cn.itcast.core.task;

import cn.itcast.core.dao.item.ItemCatDao;
import cn.itcast.core.dao.specification.SpecificationOptionDao;
import cn.itcast.core.dao.template.TypeTemplateDao;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.template.TypeTemplate;
import com.alibaba.fastjson.JSON;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
public class RedisTask {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ItemCatDao itemCatDao;

    @Resource
    private TypeTemplateDao typeTemplateDao;

    @Resource
    private SpecificationOptionDao specificationOptionDao;

    // cron：该方法执行时间的表达式
    @Scheduled(cron = "0 13 0 * * ?")
    public void autoItemCatToRedis(){
        List<ItemCat> list = itemCatDao.selectByExample(null);
        if(list != null && list.size() > 0){
            // hset key filed（分类名称-id） value（模板id）
            for (ItemCat itemCat : list) {
                redisTemplate.boundHashOps("itemCat").put(itemCat.getName(), itemCat.getTypeId());
            }
            System.out.println("定时器执行啦。。。。1");
        }
    }

    @Scheduled(cron = "0 10 0 * * ?")
    public void autoBrandsAndSpecsCatToRedis(){
        // 列表查询的过程中将数据同步到redis中
        List<TypeTemplate> list = typeTemplateDao.selectByExample(null);
        if(list != null && list.size() > 0){
            for (TypeTemplate template : list) {
                // [{"id":37,"text":"花花公子"},{"id":38,"text":"七匹狼"}]
                // 将品牌结果集存储到内存中
                String brandIds = template.getBrandIds();
                List<Map> brandList = JSON.parseArray(brandIds, Map.class); // 品牌结果集
                redisTemplate.boundHashOps("brandList").put(template.getId(), brandList);
                // 将规格结果集存储到内存中
                List<Map> specList = findBySpecList(template.getId());
                redisTemplate.boundHashOps("specList").put(template.getId(), specList);
            }
            System.out.println("定时器执行啦。。。。2");
        }
    }

    public List<Map> findBySpecList(Long id) {
        // 通过id获取到模板
        TypeTemplate typeTemplate = typeTemplateDao.selectByPrimaryKey(id);
        // 通过模板获取规格
        // [{"id":27,"text":"网络"},{"id":32,"text":"机身内存"}]
        String specIds = typeTemplate.getSpecIds();
        // 将字符串转成对象
        List<Map> specList = JSON.parseArray(specIds, Map.class);
        // 设置规格选项
        if(specList != null && specList.size() > 0){
            for (Map map : specList) {
                // 获取规格id
                long specId = Long.parseLong(map.get("id").toString());
                // 获取对应的规格选项
                SpecificationOptionQuery query = new SpecificationOptionQuery();
                query.createCriteria().andSpecIdEqualTo(specId);
                List<SpecificationOption> options = specificationOptionDao.selectByExample(query);
                // 将规格选择设置到map中
                map.put("options", options);
            }
        }
        return specList;
    }
}
