package cn.itcast.core.service.address;

import cn.itcast.core.pojo.address.Address;

import java.util.List;

/**
 * @Auther:Jerry Lau
 * @Date: 2019/3/7 0007
 * @Description: cn.itcast.core.service.address
 * @Version: 1.0
 */
public interface AddressService {
    /**
     * 获取当前登录用户的收获地址
     * @param userId
     * @return
     */
    public List<Address> findAddressList(String userId);
}
