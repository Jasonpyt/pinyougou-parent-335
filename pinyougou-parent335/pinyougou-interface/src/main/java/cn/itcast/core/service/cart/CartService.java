package cn.itcast.core.service.cart;

import cn.itcast.core.pojo.cart.Cart;
import cn.itcast.core.pojo.item.Item;

import java.util.List;

public interface CartService {

    /**
     * 获取库存对象
     * @param id
     * @return
     */
    public Item findOne(Long id);

    /**
     * 填充购物车列表需要回显的数据
     * @param cartList
     * @return
     */
    List<Cart> setAttrituteForCart(List<Cart> cartList);

    /**
     * 将购物车同步到redis中
     * @param username
     * @param cartList
     */
    void mergeCartList(String username, List<Cart> cartList);

    /**
     * 从redis中取出购物车
     * @param username
     * @return
     */
    List<Cart> findCartListFromRedis(String username);
}
