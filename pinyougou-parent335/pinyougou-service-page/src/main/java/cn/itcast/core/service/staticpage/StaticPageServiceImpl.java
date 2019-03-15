package cn.itcast.core.service.staticpage;

import cn.itcast.core.dao.good.GoodsDao;
import cn.itcast.core.dao.good.GoodsDescDao;
import cn.itcast.core.dao.item.ItemCatDao;
import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.pojo.good.GoodsDesc;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.item.ItemCat;
import cn.itcast.core.pojo.item.ItemQuery;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticPageServiceImpl implements StaticPageService, ServletContextAware {

    @Resource
    private GoodsDao goodsDao;

    @Resource
    private GoodsDescDao goodsDescDao;

    @Resource
    private ItemDao itemDao;

    @Resource
    private ItemCatDao itemCatDao;

    // springmvc支持freemarker的
    // 注入FreeMarkerConfigurer：获取需要的Configuration并且可以指定模板位置
    private Configuration configuration;
    // 属性注入
    public void setFreeMarkerConfigurer(FreeMarkerConfigurer freeMarkerConfigurer) {
        this.configuration = freeMarkerConfigurer.getConfiguration();
    }

    private ServletContext servletContext;
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * 生成商品详情静态页
     * @param id
     */
    @Override
    public void getHtml(Long id) {

        try {
            // 1、创建Configuration并且指定模板的位置
            // 指定模板位置：硬编码了  new 对象，指定位置 --- 不合适。
            // 想这样指定：在配置文件中指定模板的位置（同springmvc指定视图地址一样）
            // 2、获取该位置下需要的模板
            Template template = configuration.getTemplate("item.ftl");
            // 3、准备数据（静态页需要的数据）
            Map<String, Object> dataModel = getDataModel(id);
            // 指定file：生成静态页的位置---可以直接访问 --- 发布到项目的真实路径下
//            request.getRealPath("/xxx"); // 此路不通
            String pathname = "/" + id + ".html";
            String path = servletContext.getRealPath(pathname);
            File file = new File(path);
            // 4、模板 + 数据 =  输出
            template.process(dataModel, new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 静态页需要的数据
    private Map<String,Object> getDataModel(Long id) {
        Map<String,Object> map = new HashMap<>();
        // 商品副标题
        Goods goods = goodsDao.selectByPrimaryKey(id);
        map.put("goods", goods);
        // 商品图片、介绍等
        GoodsDesc goodsDesc = goodsDescDao.selectByPrimaryKey(id);
        map.put("goodsDesc", goodsDesc);
        // 商品库存
        ItemQuery itemQuery = new ItemQuery();
        itemQuery.createCriteria().andGoodsIdEqualTo(id).
                andStatusEqualTo("1").andNumGreaterThan(0);
        List<Item> itemList = itemDao.selectByExample(itemQuery);
        map.put("itemList", itemList);
        // 商品分类
        ItemCat itemCat1 = itemCatDao.selectByPrimaryKey(goods.getCategory1Id());
        ItemCat itemCat2 = itemCatDao.selectByPrimaryKey(goods.getCategory2Id());
        ItemCat itemCat3 = itemCatDao.selectByPrimaryKey(goods.getCategory3Id());
        map.put("itemCat1", itemCat1);
        map.put("itemCat2", itemCat2);
        map.put("itemCat3", itemCat3);

        return map;
    }


}
