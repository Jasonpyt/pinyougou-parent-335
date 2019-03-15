package cn.itcast.core.service.seller;

import cn.itcast.core.dao.seller.SellerDao;
import cn.itcast.core.pojo.seller.Seller;
import cn.itcast.core.pojo.seller.SellerQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class SellerServiceImpl implements SellerService {

    @Resource
    private SellerDao sellerDao;

    /**
     * 商家的入驻申请
     * @param seller
     */
    @Transactional
    @Override
    public void add(Seller seller) {
        // 初始化该商家审核的状态：待审核-0
        seller.setStatus("0");
        seller.setCreateTime(new Date());
        // 需要对密码进行加密：md5、加盐（盐值）spring
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String password = bCryptPasswordEncoder.encode(seller.getPassword());
        seller.setPassword(password);
        sellerDao.insertSelective(seller);
    }

    /**
     * 待审核商家的列表查询
     * @param page
     * @param rows
     * @param seller
     * @return
     */
    @Override
    public entity.PageResult search(Integer page, Integer rows, Seller seller) {
        PageHelper.startPage(page, rows);

        SellerQuery query = new SellerQuery();

        SellerQuery.Criteria criteria = query.createCriteria();
       if (seller != null) {
           if (seller.getSellerId() != null && seller.getSellerId().length()>0) {
               criteria.andStatusEqualTo(seller.getStatus());
           }

           if (seller.getName() != null && seller.getName().length()>0) {
               criteria.andNameLike("%" + seller.getName() + "%");
           }

           if (seller.getNickName() != null && seller.getNickName().length()>0){
               criteria.andNameLike("%"+seller.getNickName()+"%");
           }

           if (seller.getStatus() != null && seller.getStatus().length()>0){
               criteria.andStatusLike("%"+seller.getStatus()+"%");
           }
       }
           Page<Seller> p = (Page<Seller>) sellerDao.selectByExample(query);

            return new entity.PageResult(p.getTotal(), p.getResult());

        }



    /**
     * 回显商家
     * @param sellerId
     * @return
     */
    @Override
    public Seller findOne(String sellerId) {
        return sellerDao.selectByPrimaryKey(sellerId);
    }

    /**
     * 审核商家
     * @param sellerId
     * @param status
     */
    @Transactional
    @Override
    public void updateStatus(String sellerId, String status) {
        Seller seller = new Seller();
        seller.setSellerId(sellerId);
        seller.setStatus(status);
        sellerDao.updateByPrimaryKeySelective(seller);
    }

    /**
     * 查询全部信息
     * @return
     */
    @Override
    public List<Seller> findAll() {
        return sellerDao.selectByExample(null);
    }
}
