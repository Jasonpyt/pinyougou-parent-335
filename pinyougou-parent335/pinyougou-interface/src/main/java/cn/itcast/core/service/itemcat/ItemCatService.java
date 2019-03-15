package cn.itcast.core.service.itemcat;

import cn.itcast.core.pojo.item.ItemCat;

import java.util.List;

public interface ItemCatService {

    /**
     * 商品分类的列表查询
     * @param parentId
     * @return
     */
    public List<ItemCat> findByParentId(Long parentId);

    /**
     * 回显模板
     * @param id
     * @return
     */
    ItemCat findOne(Long id);

    /**
     * 商品列表回显分类信息
     * @return
     */
    List<ItemCat> findAll();
}
