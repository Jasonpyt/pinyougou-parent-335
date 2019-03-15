package cn.itcast.core.service.spec;

import cn.itcast.core.dao.specification.SpecificationDao;
import cn.itcast.core.dao.specification.SpecificationOptionDao;
import cn.itcast.core.pojo.specification.Specification;
import cn.itcast.core.pojo.specification.SpecificationOption;
import cn.itcast.core.pojo.specification.SpecificationOptionQuery;
import cn.itcast.core.pojo.specification.SpecificationQuery;
import cn.itcast.core.vo.SpecVo;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class SpecServiceImpl implements SpecService {

    @Resource
    private SpecificationDao specificationDao;

    @Resource
    private SpecificationOptionDao specificationOptionDao;

    /**
     * 规格的列表查询
     * @param page
     * @param rows
     * @param specification
     * @return
     */
    @Override
    public entity.PageResult search(Integer page, Integer rows, Specification specification) {
        // 1、设置分页条件
        PageHelper.startPage(page, rows);
        // 2、设置查询条件
        SpecificationQuery query = new SpecificationQuery();
        if(specification.getSpecName() != null && !"".equals(specification.getSpecName().trim())){
            query.createCriteria().andSpecNameLike("%" + specification.getSpecName() + "%");
        }
        query.setOrderByClause("id desc");
        // 3、根据条件条件
        Page<Specification> p = (Page<Specification>) specificationDao.selectByExample(query);
        // 4、封装结果
        return new entity.PageResult(p.getTotal(), p.getResult());
    }

    /**
     * 保存规格
     * @param specVo
     */
    @Transactional
    @Override
    public void add(SpecVo specVo) {
        // 保存规格
        Specification specification = specVo.getSpecification();
        // 返回自增主键的id，需要在映射文件中去配置
        specificationDao.insertSelective(specification);
        // 保存规格选项
        List<SpecificationOption> specificationOptionList = specVo.getSpecificationOptionList();
        if(specificationOptionList != null && specificationOptionList.size() > 0){
            // 设置外键
            for (SpecificationOption specificationOption : specificationOptionList) {
                specificationOption.setSpecId(specification.getId());
                // 保存：一个个的保存
//                specificationOptionDao.insertSelective(specificationOption);
            }
            // 批量保存
            specificationOptionDao.insertSelectives(specificationOptionList);
        }
    }

    /**
     * 回显规格
     * @param id
     * @return
     */
    @Override
    public SpecVo findOne(Long id) {
        SpecVo specVo = new SpecVo();
        // 获取规格
        Specification specification = specificationDao.selectByPrimaryKey(id);
        specVo.setSpecification(specification);
        // 获取该规格下的规格选项
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(id);
        List<SpecificationOption> specificationOptionList = specificationOptionDao.selectByExample(query);
        specVo.setSpecificationOptionList(specificationOptionList);
        return specVo;
    }

    /**
     * 更新规格
     * @param specVo
     */
    @Transactional
    @Override
    public void update(SpecVo specVo) {
        // 更新规格
        Specification specification = specVo.getSpecification();
        specificationDao.updateByPrimaryKeySelective(specification);
        // 更新规格选项：先删除再添加
        // 先删除
        SpecificationOptionQuery query = new SpecificationOptionQuery();
        query.createCriteria().andSpecIdEqualTo(specification.getId());
        specificationOptionDao.deleteByExample(query);
        // 再添加
        List<SpecificationOption> specificationOptionList = specVo.getSpecificationOptionList();
        if(specificationOptionList != null && specificationOptionList.size() > 0){
            for (SpecificationOption specificationOption : specificationOptionList) {
                specificationOption.setSpecId(specification.getId());
            }
            specificationOptionDao.insertSelectives(specificationOptionList);
        }
    }

    /**
     * 删除规格
     * @param ids
     */
    @Override
    public void delete(Long[] ids) {
        if(ids != null && ids.length > 0){
            // 批量删除
            for (Long id : ids) {
                // 删除规格选项（外键的）
                SpecificationOptionQuery query = new SpecificationOptionQuery();
                query.createCriteria().andSpecIdEqualTo(id);
                specificationOptionDao.deleteByExample(query);
                // 删除规格
                specificationDao.deleteByPrimaryKey(id);
            }
        }
    }

    /**
     * 新增模板时加载规格结果集
     * @return
     */
    @Override
    public List<Map> selectOptionList() {
        return specificationDao.selectOptionList();
    }
}
