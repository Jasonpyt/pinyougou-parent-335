package cn.itcast.core.service.search;

import java.util.Map;

public interface ItemSearchService {

    /**
     * 前台系统的检索
     * @param searchMap
     * @return
     */
    public Map<String, Object> search(Map<String, String> searchMap);

    /**
     * 商品上架
     * @param id
     */
    public void isShow(Long id);

    /**
     *
     * @Title: deleteItemFromSolr
     * @Description: 删除索引库中商品
     * @param id
     * @return void
     * @throws
     */
    public void deleteItemFromSolr(Long id);
}
