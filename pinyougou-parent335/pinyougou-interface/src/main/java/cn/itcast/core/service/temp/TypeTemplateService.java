package cn.itcast.core.service.temp;


import cn.itcast.core.pojo.template.TypeTemplate;

import java.util.List;
import java.util.Map;

public interface TypeTemplateService {

    /**
     * 模板的列表查询
     * @param page
     * @param rows
     * @param typeTemplate
     * @return
     */
    public entity.PageResult search(Integer page, Integer rows, TypeTemplate typeTemplate);

    /**
     * 保存模板
     * @param typeTemplate
     */
    public void add(TypeTemplate typeTemplate);

    /**
     * 回显品牌以及扩展属性
     * @param id
     * @return
     */
    TypeTemplate findOne(Long id);

    /**
     * 回显规格以及规格选项
     * @param id
     * @return
     */
    public List<Map> findBySpecList(Long id);
}
