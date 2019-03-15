package cn.itcast.core.service.order;

import cn.itcast.core.dao.item.ItemDao;
import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.dao.order.OrderDao;
import cn.itcast.core.dao.order.OrderItemDao;
import cn.itcast.core.pojo.cart.Cart;
import cn.itcast.core.pojo.item.Item;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.utils.uniquekey.IdWorker;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderDao orderDao;

    @Resource
    private OrderItemDao orderItemDao;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private IdWorker idWorker;

    @Resource
    private ItemDao itemDao;

    //注入支付日志接口代理对象
    @Resource
    private PayLogDao payLogDao;

    /**
     * 提交订单
     * @param username
     * @param order
     */
    @Override
    public void add(String username, Order order) {
        // 保存订单：已商家为单位（购物车也是以商家为单位）
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("BUYER_CART").get(username);

        //定义集合，封装订单号
        List<String> orderIds = new ArrayList<>();

        Double payment = 0d;
        if(cartList != null && cartList.size() > 0){

            //一个cart对应一个商家
            for (Cart cart : cartList) {

                long orderId = idWorker.nextId();

                orderIds.add(orderId+"");

                order.setOrderId(orderId);  // 订单主键
                // 订单金额该商家下所有的订单金额
                order.setPaymentType("1");  // 支付类型：在线支付
                order.setStatus("1");       // 订单状态：未付款
                order.setCreateTime(new Date());
                order.setUpdateTime(new Date());
                order.setUserId(username);  // 下单用户
                order.setSourceType("2");   // 订单来源：pc端
                order.setSellerId(cart.getSellerId());  // 商家id

                // 保存订单明细
                List<OrderItem> orderItemList = cart.getOrderItemList();
                if(orderItemList != null && orderItemList.size() > 0){
                    for (OrderItem orderItem : orderItemList) {
                        long id = idWorker.nextId();
                        orderItem.setId(id);
                        Item item = itemDao.selectByPrimaryKey(orderItem.getItemId());
                        orderItem.setGoodsId(item.getGoodsId());    // 商品id
                        orderItem.setOrderId(orderId);              // 订单id
                        orderItem.setTitle(item.getTitle());        // 标题
                        orderItem.setPrice(item.getPrice());        // 单价
                        double totalFee = item.getPrice().doubleValue() * orderItem.getNum();
                        payment += totalFee;
                        orderItem.setTotalFee(new BigDecimal(totalFee));            // 小计
                        orderItem.setPicPath(item.getImage());      // 商品图片
                        orderItem.setSellerId(item.getSellerId());  // 商家id
                        orderItemDao.insertSelective(orderItem);
                    }
                }
                // 设置该商家下的总金额
                order.setPayment(new BigDecimal(payment));
                orderDao.insertSelective(order);
            }
        }

        //订单支付日志保存
        PayLog payLog = new PayLog();
        //补全属性
        //交易流水号
        payLog.setOutTradeNo(idWorker.nextId()+"");

        //用户id
        payLog.setUserId(username);

        //创建时间
        payLog.setCreateTime(new Date());

        //支付金额
        payLog.setTotalFee(payment.longValue());

        //支付状态
        payLog.setTradeState("0");
        //支付类型 在线支付
        payLog.setPayType("1");

        //订单号
        payLog.setOrderList(orderIds.toString().replace("[","").replace("]","")); // 3,6

        //设置完成，报错订单支付日志
        payLogDao.insertSelective(payLog);

        //把交易日志存储redis
        redisTemplate.boundHashOps("payLog").put(username,payLog);



        // 删除购物车
        redisTemplate.boundHashOps("BUYER_CART").delete(username);
    }
}
