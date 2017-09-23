package org.chengy.infrastructure.music163secret;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by nali on 2017/9/15.
 */
public class Music163ApiCons {


	public static final String communityName="163music";

	public static final String Music163UserHost = "https://music.163.com/user/home?id=";


	public static final String fansUrl = "https://music.163.com/weapi/user/getfolloweds?csrf_token=";
	public static final String songUrl = "https://music.163.com/weapi/song/enhance/player/url?csrf_token=";

	public static final String songRecordUrl = "https://music.163.com/weapi/v1/play/record?csrf_token=";

	public static String getFansParams(String uid, int pageIndex, int pageSize) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String offset = String.valueOf((pageIndex - 1) * pageSize);
		FansParam fansParam = new Music163ApiCons().new FansParam(uid, offset, "true", String.valueOf(pageSize), "");
		String jsonParam = objectMapper.writeValueAsString(fansParam);
		return jsonParam;
	}


	public static String getFollowedParams(String uid, int pageIndex, int pageSize) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String offset = String.valueOf((pageIndex - 1) * pageSize);
		FollowedParam followedParam = new Music163ApiCons().new FollowedParam(uid, offset, "true", String.valueOf(pageSize), "");
		String jsonParam = objectMapper.writeValueAsString(followedParam);
		return jsonParam;
	}

	public static String getFollowedUrl(String uid) {
		String followedUrl = "https://music.163.com/weapi/user/getfollows/";
		return followedUrl + uid + "?csrf_token=";
	}

	public static String getSongRecordALLParams(String uid, int pageIndex, int pageSize) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String offset = String.valueOf((pageIndex - 1) * pageSize);
		return objectMapper.writeValueAsString(new Music163ApiCons().new LoveSongParamAllTime(uid, offset, "true", String.valueOf(pageSize), ""));

	}


	/**
	 * 请求关注人的参数内部类
	 */
	class FollowedParam {

		private String uid;
		private String offset;
		private String total;
		private String limit;
		private String csrf_token;

		public FollowedParam(String uid, String offset, String total, String limit, String csrf_token) {
			this.uid = uid;
			this.offset = offset;
			this.total = total;
			this.limit = limit;
			this.csrf_token = csrf_token;
		}


		public String getUid() {
			return uid;
		}

		public void setUid(String uid) {
			this.uid = uid;
		}

		public String getOffset() {
			return offset;
		}

		public void setOffset(String offset) {
			this.offset = offset;
		}

		public String getTotal() {
			return total;
		}

		public void setTotal(String total) {
			this.total = total;
		}

		public String getLimit() {
			return limit;
		}

		public void setLimit(String limit) {
			this.limit = limit;
		}

		public String getCsrf_token() {
			return csrf_token;
		}

		public void setCsrf_token(String csrf_token) {
			this.csrf_token = csrf_token;
		}
	}

	/**
	 * 获取最近一周喜爱歌曲的参数
	 */
	class LoveSongParamWeek extends FollowedParam {
		private int type = 1;

		public LoveSongParamWeek(String uid, String offset, String total, String limit, String csrf_token) {
			super(uid, offset, total, limit, csrf_token);
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}
	}

	/**
	 * 获取所有时间最爱的歌曲
	 */
	class LoveSongParamAllTime extends FollowedParam {
		private int type = 0;

		public LoveSongParamAllTime(String uid, String offset, String total, String limit, String csrf_token) {
			super(uid, offset, total, limit, csrf_token);
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}
	}


	/**
	 * 请求粉丝的参数内部类
	 */
	class FansParam {
		private String userId;
		private String offset;
		private String total;
		private String limit;
		private String csrf_token;

		public FansParam(String userId, String offset, String total, String limit, String csrf_token) {
			this.userId = userId;
			this.offset = offset;
			this.total = total;
			this.limit = limit;
			this.csrf_token = csrf_token;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getOffset() {
			return offset;
		}

		public void setOffset(String offset) {
			this.offset = offset;
		}

		public String getTotal() {
			return total;
		}

		public void setTotal(String total) {
			this.total = total;
		}

		public String getCsrf_token() {
			return csrf_token;
		}

		public void setCsrf_token(String csrf_token) {
			this.csrf_token = csrf_token;
		}

		public String getLimit() {
			return limit;
		}

		public void setLimit(String limit) {
			this.limit = limit;
		}
	}
}
