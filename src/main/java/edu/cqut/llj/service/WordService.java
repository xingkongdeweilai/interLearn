package edu.cqut.llj.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;

import edu.cqut.llj.dao.WordDao;
import edu.cqut.llj.enums.ResultEnum;
import edu.cqut.llj.exception.GirlException;
import edu.cqut.llj.pojo.Word;
import edu.cqut.llj.pojo.WordAndUser;
import edu.cqut.llj.properties.WordProperties;
import edu.cqut.llj.utils.CommonUtil;
import edu.cqut.llj.vo.ThreeWordExample;
import edu.cqut.llj.vo.WordAndWordExample;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import tk.mybatis.mapper.entity.Example;

@Service
public class WordService {
	
	@Autowired
	private WordDao wordDao;
	@Autowired
	private WordProperties wordProperties;
	private static final Logger logger = LoggerFactory.getLogger(WordService.class);

	/**
	 * 查询所有单词及例句
	 * @return
	 */
	public JSONArray queryWordList() {
		List<Word> list = wordDao.queryWordList();
		JSONArray wordJson = JSONArray.fromObject(list);
		logger.info(wordJson.toString());
		return wordJson;
	}

	public Word updateWord(Word word) {
		return wordDao.updateWord(word);
	}
	
//	public JSONArray queryExampleById(Integer word_id) {
//		List<WordExample> list = wordDao.queryExampleById(word_id);
//		JSONArray wordJson = JSONArray.fromObject(list);
//		return wordJson;
//	}

	public JSONObject queryWordById(Integer word_id){
		Word word = wordDao.queryWordById(word_id);
		return JSONObject.fromObject(word);
	}

	public int addNewWord(Word word) {
		int result = 0;
		//去掉wordname两端空白
		word.setWordname(word.getWordname().trim());
		if("".equals(word.getWordname())){
			throw new GirlException(ResultEnum.ADD_WORDNAME_NULL);
		}
		try{
			result = wordDao.addNewWord(word);
		}catch(Exception e){
			throw new GirlException(ResultEnum.ADD_WORD_ERROR);
		}
		return result;
	}

	/**
	 * 分页查询单词
	 * @param word 
	 * @param page
	 * @param limit
	 * @return
	 */
	public JSONArray queryWordListPaged(Word word, Integer page, Integer limit) {
		PageHelper.startPage(page,limit);
		Example example = new Example(Word.class);
		Example.Criteria criteria = example.createCriteria();
		criteria.andEqualTo("status", 0);
		example.orderBy("wordname").asc();
		List<Word> list = wordDao.queryWordListPaged(example);
		return JSONArray.fromObject(list);
	}

	/**
	 * 查询wordList的总长度
	 * @return
	 */
	public Integer getWordListSize() {
		return wordDao.getWordListSize();
	}
	
	/**
	 * 删除一个单词
	 * @param word
	 * @return
	 */
	public boolean deleteWord(Word word){
		return wordDao.deleteWord(word);
	}

	/**
	 * 返回每日单词，三个查询方案
	 * 1、在word_and_user查询1/2，查询条件：relation<3，即正在学习中的词
	 * 2、在word中查询，查询量补齐，查询条件：在word_and_user中relation<3
	 * @param user_id
	 * @param lastWordList 
	 * @return
	 */
	public JSONObject getRememberWord(Integer user_id, JSONObject lastWordList) {
		//判断今天是否已经获取过当日单词，是:直接返回上次的json,否则请求新的json
		if(lastWordList!=null
				&&lastWordList.get("getNow")!=null
				&&lastWordList.get("getNow").toString().equals(CommonUtil.getNow())){
			return lastWordList;
		}
		
		Integer size1 = 0;
		List<Word> plan1 = new ArrayList<>();
		List<Word> plan2 = new ArrayList<>();
		Integer wordNumber = Integer.parseInt(wordProperties.getEveryWord());
		plan1 = wordDao.queryLearningWord(user_id,wordNumber/2);
		size1 = plan1.size();
		plan2 = wordDao.queryNewWord(wordNumber-size1);
		
		plan1.addAll(plan2);
		JSONArray todayWordList = JSONArray.fromObject(plan1);
		JSONObject resultJson = new JSONObject();
		resultJson.put("todayWordList", todayWordList);
		resultJson.put("getNow", CommonUtil.getNow());
		logger.info(resultJson.toString());
		return resultJson;
	}

	/**
	 * 更新用户与单词的relationship
	 * 先查询中间表word_and_user中是否有用户与单词的联系
	 * 如果没有，中间表添加一条新数据
	 * 如果有，且relationship<3,relationship+1，没有，不操作
	 * @param user_id
	 * @param word_id
	 * @return
	 */
	public boolean updateWordUserRelation(Integer user_id, Integer word_id) {
		WordAndUser wu = wordDao.queryRelation(user_id,word_id);
		//没有联系就新增
		if(wu==null){
			wu = new WordAndUser();
			wu.setUser_id(user_id);
			wu.setWord_id(word_id);
			wu.setRelationship(0);
			wordDao.addRelation(wu);
		//正在学的单词relationship+1
		}else if(wu.getRelationship()<3){
			logger.info(wu.toString());
			wu.setRelationship(wu.getRelationship()+1);
			logger.info(wu.toString());
			wordDao.updateWordUserRelation(wu);
		}
		return true;
	}
	
//	public ThreeWordExample updateWordExample(String cn,String en, Integer word_id) {
//		return wordDao.updateWordExample(cn,en, word_id);
//	}
	
	/*public WordAndWordExample queryDetailById(Integer word_id) {
		return wordDao.queryDetailById(word_id);
	}*/

	
	
	
//	public List<Word> queryWordListPaged(Word word, Integer page, Integer pageSize) {
//		//开始分页
//		PageHelper.startPage(page,pageSize);
//		
//		Example example = new Example(Word.class);
////		Example.Criteria criteria = example.createCriteria();
//		
////		if (!StringUtils.isEmptyOrWhitespace(word.getWordname())) {
////			criteria.andLike("nickname", "%" + word.getWordname() + "%");
////		}
////		example.orderBy("registTime").desc();
//		
//		return wordDao.queryWordListPaged(example);
//	}

//	public List<WordAndWordExample> test() {
//		return wordDao.test();
//	}

}
