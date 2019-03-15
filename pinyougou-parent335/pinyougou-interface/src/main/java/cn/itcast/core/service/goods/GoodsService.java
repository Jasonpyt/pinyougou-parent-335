package cn.itcast.core.service.goods;

import cn.itcast.core.pojo.good.Goods;
import cn.itcast.core.vo.GoodsVo;

public interface GoodsService {

    /**
     * 保存商品
     * @param goodsVo
     */
    public void add(GoodsVo goodsVo);

    /**
     * 商家系统下的商品列表查询
     * @param page
     * @param rows
     * @param goods
     * @return
     */
    public entity.PageResult search(Integer page, Integer rows, Goods goods);

    /**
     * 回显商品信息
     * @param id
     * @return
     */
    public GoodsVo findOne(Long id);

    /**
     * 更新商品
     * @param goodsVo
     */
    public void update(GoodsVo goodsVo);

    /**
     * 运营商系统下的商品列表查询
     * @param page
     * @param rows
     * @param goods
     * @return
     */
    public entity.PageResult searchForManager(Integer page, Integer rows, Goods goods);

    /**
     * 商品审核
     * @param ids
     * @param auditStatus
     */
    public void updateStatus(Long[] ids, String auditStatus);

    /**
     * 商品删除
     * @param ids
     */
    public void delete(Long[] ids);
}
