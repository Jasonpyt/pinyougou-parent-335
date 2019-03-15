package cn.itcast.core.service.cart;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.dao.seller.SellerDao;
import cn.itcast.core.pojo.cart.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.pojo.seller.Seller;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Resource
    private ItemDao itemDao;

    @Resource
    private SellerDao sellerDao;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取库存对象
     * @param id
     * @return
     */
    @Override
    public Item findOne(Long id) {
        return itemDao.selectByPrimaryKey(id);
    }

    /**
     * 填充购物车列表需要回显的数据
     * @param cartList
     * @return
     */
    @Override
    public List<Cart> setAttrituteForCart(List<Cart> cartList) {
        for (Cart cart : cartList) {
            // 填充商家的店铺名称
            Seller seller = sellerDao.selectByPrimaryKey(cart.getSellerId());
            cart.setSellerName(seller.getNickName());
            // 填充购物项的数据：图片、标题、单价、计算出小计
            List<OrderItem> orderItemList = cart.getOrderItemList();
            for (OrderItem orderItem : orderItemList) {
                Item item = itemDao.selectByPrimaryKey(orderItem.getItemId());
                orderItem.setPicPath(item.getImage());  // 商品图片
                orderItem.setTitle(item.getTitle());    // 商品标题
                orderItem.setPrice(item.getPrice());    // 商品单价
                // 小计 = 单价 * 数量
                BigDecimal totalFee = new BigDecimal(item.getPrice().doubleValue() * orderItem.getNum());
                orderItem.setTotalFee(totalFee);        // 商品小计
            }
        }
        return cartList;
    }

    /**
     * 将购物车同步到redis中
     * @param username
     * @param newCartList
     */
    @Override
    public void mergeCartList(String username, List<Cart> newCartList) {
        // 1、取出老车
        List<Cart> oldCartList = (List<Cart>) redisTemplate.boundHashOps("BUYER_CART").get(username);
        // 2、将新车和老车进行合并（新车合并到老车中）
        oldCartList = mergeNewCartListToOldCartList(newCartList, oldCartList);
        // 3、将老车进行保存
        redisTemplate.boundHashOps("BUYER_CART").put(username, oldCartList);
    }

    /**
     * 从redis中取出购物车
     * @param username
     * @return
     */
    @Override
    public List<Cart> findCartListFromRedis(String username) {
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("BUYER_CART").get(username);
        return cartList;
    }

    // 新车合并到老车中
    private List<Cart> mergeNewCartListToOldCartList(List<Cart> newCartList, List<Cart> oldCartList) {
        if(newCartList != null){
            if(oldCartList != null){
                // 新车、老车都不为空。开始合并
                for (Cart newCart : newCartList) {
                    // 判断是否是同一个商家
                    int sellerIndexOf = oldCartList.indexOf(newCart);
                    if(sellerIndexOf != -1){
                        // 同一个商家：继续判断是否是同款商品
                        List<OrderItem> oldOrderItemList = oldCartList.get(sellerIndexOf).getOrderItemList();
                        List<OrderItem> newOrderItemList = newCart.getOrderItemList();
                        for (OrderItem newOrderItem : newOrderItemList) {
                            int itemIndexOf = oldOrderItemList.indexOf(newOrderItem);
                            if(itemIndexOf != -1){
                                // 同款商品：合并数量
                                Integer newNum = newOrderItem.getNum();
                                OrderItem oldOrderItem = oldOrderItemList.get(itemIndexOf);
                                oldOrderItem.setNum(oldOrderItem.getNum() + newNum);
                            }else{
                                // 不是同款商品：是同一个商家。将该购物项加入到该商家下的购物项集中
                                oldOrderItemList.add(newOrderItem);
                            }
                        }
                    }else{
                        // 不是同一个商家：直接装车
                        oldCartList.add(newCart);
                    }
                }
            }else{
                return newCartList;
            }
        }else{
            // 新车为null，直接返回老车
            return oldCartList;
        }
        return oldCartList;
    }
}
