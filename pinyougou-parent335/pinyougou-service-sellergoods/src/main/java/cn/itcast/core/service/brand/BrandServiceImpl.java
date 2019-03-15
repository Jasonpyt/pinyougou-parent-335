package cn.itcast.core.service.brand;

import cn.itcast.core.dao.good.BrandDao;
import cn.itcast.core.pojo.good.Brand;
import cn.itcast.core.pojo.good.BrandQuery;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class BrandServiceImpl implements BrandService {

//    @Autowired
    // 好处：
    // 框架对外提供的服务越多性能越低
    // 降低程序的耦合度
    @Resource
    private BrandDao brandDao;

    /**
     * 查询所有的品牌
     * @return
     */
    @Override
    public List<Brand> findAll() {
        List<Brand> brands = brandDao.selectByExample(null);
        return brands;
    }

    /**
     * 品牌管理结果分页
     * @param pageNo
     * @param pageSize
     * @return
     */
    @Override
    public entity.PageResult findPage(Integer pageNo, Integer pageSize) {
        // 1、通过分页助手设置其始行
        PageHelper.startPage(pageNo, pageSize);
        // 2、查询
        Page<Brand> page = (Page<Brand>) brandDao.selectByExample(null);
        // 3、将结果封装到PageResult对象中
        return new entity.PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 条件查询
     * @param pageNo
     * @param pageSize
     * @param brand
     * @return
     */
    @Override
    public entity.PageResult search(Integer pageNo, Integer pageSize, Brand brand) {
        // 1、设置分页条件
        PageHelper.startPage(pageNo, pageSize);
        // 2、设置查询条件
        BrandQuery brandQuery = new BrandQuery();
        // 通过query封装查询条件
        BrandQuery.Criteria criteria = brandQuery.createCriteria();
        if(brand.getName() != null && !"".equals(brand.getName().trim())){
            criteria.andNameLike("%" + brand.getName() + "%");
        }
        if(brand.getFirstChar() != null && !"".equals(brand.getFirstChar().trim())){
            criteria.andFirstCharEqualTo(brand.getFirstChar());
        }
        // 根据id排序
        brandQuery.setOrderByClause("id desc");
        // 3、根据条件查询
        Page<Brand> page = (Page<Brand>) brandDao.selectByExample(brandQuery);
        // 4、将结果封装到PageResult对象中
        return new entity.PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 保存品牌
     * @param brand
     */
    @Transactional
    @Override
    public void add(Brand brand) {
        brandDao.insertSelective(brand);
    }

    /**
     * 回显品牌
     * @param id
     * @return
     */
    @Override
    public Brand findOne(Long id) {
        return brandDao.selectByPrimaryKey(id);
    }

    /**
     * 更新品牌
     * @param brand
     */
    @Transactional
    @Override
    public void update(Brand brand) {
        brandDao.updateByPrimaryKeySelective(brand);
    }

    /**
     * 批量删除品牌
     * @param ids
     */
    @Transactional
    @Override
    public void delete(Long[] ids) {
        if(ids != null && ids.length > 0){
            // 并不是批量
            // 执行的sql语句效率不高
//            for (Long id : ids) {
//                brandDao.deleteByPrimaryKey(id);
//            }
            // 场景：导入报表到数据库中（内存溢出）
            brandDao.deleteByPrimaryKeys(ids);
        }
    }

    /**
     * 新增模板时需要加载的品牌结果集
     * @return
     */
    @Override
    public List<Map> selectOptionList() {
        return brandDao.selectOptionList();
    }
}
