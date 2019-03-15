package cn.itcast.core.service.pay;

import java.util.Map;

/**
 * @Auther:Jerry Lau
 * @Date: 2019/3/9 0009
 * @Description: cn.itcast.core.service.pay
 * @Version: 1.0
 */
public interface WxPayService {
    /**
     *生成二维码
     * @param userId
     * @return
     */
    public Map createNative(String userId);

    /**
     * 查询订单状态
     * @param out_trade_no
     * @return
     */
    public Map queryPayStatus(String out_trade_no);
}
