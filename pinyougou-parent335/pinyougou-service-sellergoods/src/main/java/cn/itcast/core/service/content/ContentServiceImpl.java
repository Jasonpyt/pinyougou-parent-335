package cn.itcast.core.service.content;

import java.util.List;

import cn.itcast.core.pojo.ad.ContentQuery;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import cn.itcast.core.dao.ad.ContentDao;
import cn.itcast.core.pojo.ad.Content;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

@Service
public class ContentServiceImpl implements ContentService {

	@Resource
	private ContentDao contentDao;

	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Override
	public List<Content> findAll() {
		List<Content> list = contentDao.selectByExample(null);
		return list;
	}

	@Override
	public entity.PageResult findPage(Content content, Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<Content> page = (Page<Content>)contentDao.selectByExample(null);
		return new entity.PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void add(Content content) {
	    // 清空缓存
        clearCache(content.getCategoryId());
		contentDao.insertSelective(content);
	}



    @Override
	public void edit(Content content) {
	    // 更新操作，清空缓存
        // 判断广告的分类是否发生改变
        // 如果分类id发生改变：将之前的分类和现在的分类都要清空
        Long newCategoryId = content.getCategoryId();
        Long oldCategoryId = contentDao.selectByPrimaryKey(content.getId()).getCategoryId(); // 未更新前
        if(newCategoryId != oldCategoryId){
            // 分类id发生改变
            clearCache(oldCategoryId);
            clearCache(newCategoryId);
        }else{
            clearCache(oldCategoryId);
        }
        contentDao.updateByPrimaryKeySelective(content);
	}

	@Override
	public Content findOne(Long id) {
		Content content = contentDao.selectByPrimaryKey(id);
		return content;
	}

	@Override
	public void delAll(Long[] ids) {
		if(ids != null){
			for(Long id : ids){
			    clearCache(contentDao.selectByPrimaryKey(id).getCategoryId());
				contentDao.deleteByPrimaryKey(id);
			}
		}
	}

	// 清空缓存
    private void clearCache(Long categoryId) {
	    redisTemplate.boundHashOps("content").delete(categoryId);
    }

	/**
	 * 首页大广告的轮播图
	 * @param categoryId
	 * @return
	 */
    @Override
    public List<Content> findByCategoryId(Long categoryId) {
        // 判断缓存中是否有广告
        List<Content> list = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
        // 缓存穿透（击穿）
        if(list == null){
            synchronized (this){
                list = (List<Content>) redisTemplate.boundHashOps("content").get(categoryId);
                if(list == null){
                    // 说明缓存中没有数据，从数据库中查询
                    // 设置查询条件
                    ContentQuery contentQuery = new ContentQuery();
                    contentQuery.createCriteria().andCategoryIdEqualTo(categoryId).
                            andStatusEqualTo("1");	// 根据分类查询并且可用的广告
                    contentQuery.setOrderByClause("sort_order desc");	// 根据该字段排序
                    // 查询
                    list = contentDao.selectByExample(contentQuery);
                    // 将数据翻入到缓存中
                    redisTemplate.boundHashOps("content").put(categoryId, list);
                }
            }

        }
        return list;
    }


}
