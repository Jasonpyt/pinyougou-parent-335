package cn.itcast.core.controller.cart;

import cn.itcast.core.entity.Result;
import cn.itcast.core.pojo.cart.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.service.cart.CartService;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    // 服务器端支持跨域请求
//            response.setHeader("Access-Control-Allow-Origin", "http://localhost:9003");
//            response.setHeader("Access-Control-Allow-Credentials", "true");

    @Reference
    private CartService cartService;

    /**
     * 将商品加入购物车中
     * @param itemId
     * @param num
     * @return
     */
    // 使用@CrossOrigin注解，该属性allowCredentials = "true" 默认是true
    @RequestMapping("/addGoodsToCartList.do")
    @CrossOrigin(origins = {"http://localhost:9003"})
    public Result addGoodsToCartList(Long itemId, Integer num,
                                     HttpServletRequest request, HttpServletResponse response){
        try {

            // 具体业务实现
            // 1、定义一个空车集合
            List<Cart> cartList = null;
            boolean flag = false;   // 定义的开关（标识）
            // 2、判断本地是否有购物车
            Cookie[] cookies = request.getCookies();
            if(cookies != null && cookies.length > 0){
                for (Cookie cookie : cookies) { // 数据格式：key-value
                    if("BUYER_CART".equals(cookie.getName())){
                        // 3、有：直接取出
                        String text = cookie.getValue(); // json串
                        cartList = JSON.parseArray(text, Cart.class);
                        flag = true;    // 打开开关
//                        URLEncoder   tomcat8："" 特殊字符
//                        URLDecoder
                        break;  // 找到，跳出循环
                    }
                }
            }
            // 4、没有：需要创建一个车子
            if(cartList == null){
                cartList =  new ArrayList<>();
            }
            // ======有车了======
            // 将数据封装cart对象中：对cookie进行瘦身
            Cart cart = new Cart();
            Item item = cartService.findOne(itemId);
            cart.setSellerId(item.getSellerId()); // 商家id
            List<OrderItem> orderItemList = new ArrayList<>();
            OrderItem orderItem = new OrderItem();
            orderItem.setItemId(itemId);
            orderItem.setNum(num);
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList); // 购物项：skuId---num
            // 5、将商品装车
//            cartList.add(cart);
            // 5-1、判断商品是否属于同一个商家（本质：sellerId是否一致）
//            cartList.contains(cart);    //
            int sellerIndexOf = cartList.indexOf(cart);
            if(sellerIndexOf != -1){
                // 同一个商家，继续判断是否是同款商品
                // 将该商家下的购物项取出
                Cart oldCart = cartList.get(sellerIndexOf);
                List<OrderItem> oldOrderItemList = oldCart.getOrderItemList();
                int itemIndexOf = oldOrderItemList.indexOf(orderItem); // 判断是否是同款商品（本质：itemId）
                if(itemIndexOf != -1){
                    // 是：同款商品，合并数量
                    OrderItem oldOrderItem = oldOrderItemList.get(itemIndexOf);
                    oldOrderItem.setNum(oldOrderItem.getNum() + num);
                }else{
                    // 不是：将购物项添加到该商家下
                    oldOrderItemList.add(orderItem);
                }
            }else{
                cartList.add(cart);
            }
            // 判断用户是否登录
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("username:"+username);
            if(!"anonymousUser".equals(username)){
                // 6-1、已登录：将购物车保存到redis中
                cartService.mergeCartList(username, cartList);
                // 本地有：将本地的购物车清空
                if(flag){
                    Cookie cookie = new Cookie("BUYER_CART", null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");    // 设置cookie的共享   9003/ 9103/
                    response.addCookie(cookie);
                }

            }else{
                // 6-2、保存购物车到cookie中
                Cookie cookie = new Cookie("BUYER_CART", JSON.toJSONString(cartList));
                cookie.setMaxAge(60*60);
                cookie.setPath("/");    // 设置cookie的共享   9003/ 9103/
                response.addCookie(cookie);
            }

            return new Result(true, "添加购物车成功");
        }catch (Exception e){
            e.printStackTrace();
            return new Result(false, "添加购物车失败");
        }
    }

    /**
     * 回显购物车中的列表数据
     * @return
     */
    @RequestMapping("/findCartList.do")
    public List<Cart> findCartList(HttpServletRequest request, HttpServletResponse response){

        // 未登录：从cookie中获取
        List<Cart> cartList = null;
        Cookie[] cookies = request.getCookies();
        if(cookies != null && cookies.length > 0){
            for (Cookie cookie : cookies) { // 数据格式：key-value
                if("BUYER_CART".equals(cookie.getName())){
                    String text = cookie.getValue(); // json串
                    cartList = JSON.parseArray(text, Cart.class);
                    break;  // 找到，跳出循环
                }
            }
        }

        // 已登录：从redis中获取
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!"anonymousUser".equals(username)){
            // 场景：未登录的情况下，将上加入购物车
            // 用户去登录：登录成功后，在页面点击【我的购物车】，需要将本地的购物车同步到redis中
            if(cartList != null){
                // 同步
                cartService.mergeCartList(username, cartList);
                // 清空本地
                Cookie cookie = new Cookie("BUYER_CART", null);
                cookie.setMaxAge(0);
                cookie.setPath("/");    // 设置cookie的共享   9003/ 9103/
                response.addCookie(cookie);
            }
            cartList = cartService.findCartListFromRedis(username);
        }


        // 取出的购物车需要进行数据的填充
        if(cartList != null){
            // 填充数据
            cartList = cartService.setAttrituteForCart(cartList);
        }
        return cartList;
    }
}
