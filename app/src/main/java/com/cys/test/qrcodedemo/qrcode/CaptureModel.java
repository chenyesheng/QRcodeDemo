package com.cys.test.qrcodedemo.qrcode;

import android.content.Context;


public class CaptureModel {
	
	/**查看个人资料**/
	public static final String SCAN_TYPE_PERSONAL_INFO="YC0-";
	/**表示消费者扫商家积分二维码**/
	public static final String SCAN_TYPE_STORE_INTEGRAL="YC1-";
	/**表示消费者扫广场送积分二维码**/
	public static final String SCAN_TYPE_MALL_INTEGRAL="YC2-";
	/**表示消费者扫商家送优惠券二维码**/
	public static final String SCAN_TYPE_STORE_COUPON="YC3-";
	/**表示消费者扫广场送优惠券二维码**/
	public static final String SCAN_TYPE_MALL_COUPON="YC4-";
	/**表示商家或者广场扫描消费者的优惠券二维码**/
	public static final String SCAN_TYPE_SCAN_CUSTOMER_COUPON="YC5-";
	/**表示消费者扫描商家的赠送||抢红包||砸金蛋||漂流瓶||摇一摇||大转盘||次数的二维码**/
	public static final String SCAN_TYPE_SCAN_STORE_REDPACKET="YC6-";
	/**表示消费者扫描广场的赠送||抢红包||砸金蛋||漂流瓶||摇一摇||大转盘||次数的二维码**/
	public static final String SCAN_TYPE_SCAN_MALL_REDPACKET="YC7-";
	/**商家/广场扫描消费者的中奖优惠券**/
	public static final String SCAN_TYPE_BUS_SCAN_PRIZE_COUPON="YC8-";
	/**商家/广场扫描消费者的中奖礼品和现金**/
	public static final String SCAN_TYPE_BUS_SCAN_PRIZE_GIFT="YC9-";
	/**表示消费者扫描房产的赠送||抢红包||砸金蛋||漂流瓶||摇一摇||大转盘||次数的二维码**/
	public static final String SCAN_TYPE_SCAN_HOUSE_REDPACKET="YC10-";
	/**商家扫描消费者的消费赠送积分**/
	public static final String SCAN_TYPE_SCAN_GIVE_INTEGRAL="YC14-";
	
	private Context context;
	private static CaptureModel model;
//	/**该参数区分地扫码后不同的操作**/
//	private String type;
//	private static final String TYPE="type";
//	
//	
//	public String getType() {
//		if(type==null||type.equals("")){
//			return PreferenceUtils.getPrefString(context, TYPE, "");
//		}
//		return type;
//	}
//
//
//	public void setType(String type) {
//		this.type = type;
//		PreferenceUtils.setPrefString(context, TYPE, type);
//	}
//	

	private CaptureModel(Context context) {
		this.context = context;
	}

	
	public static CaptureModel getInstance(Context context) {
		if (model == null) {
			model = new CaptureModel(context);
		}
		return model;
	}
	
	
//	/**
//	 * 获取我的二维码信息
//	 * @param isFirst 是否是进入页面的时候获取，是进入页面的时候获取显示box的load，下拉刷新不用显示box的load
//	 */
//	private void getQrcodeInfo(boolean isFirst){
//		if(isFirst){
//			box.showLoadingLayout();
//		}
//		String url = ConnectURL.appHeadImpl("Consumer", "MyQrcode");
//		HashMap<String, Object> params = new HashMap<String, Object>();
//		params.put("app_key", BBApplication.getToken());
//		aQuery.ajax(url, params, String.class, this, "callBackGetQrcodeInfo");
//	}
//	
//	/***
//	 * 获获取我的二维码信息回调
//	 * @param url
//	 * @param object
//	 * @param status
//	 */
//	public void callBackGetQrcodeInfo(String url, String object, AjaxStatus status){
//		BBLog.println(this, " 获取我的二维码信息回调==="+object);
//		box.hideAll();
//		mSwipeLayout.setRefreshing(false);
//		if(status==null){
//			box.setInternetOffMessage(getResources().getString(R.string.getdata_Failure));
//			box.showInternetOffLayout();
//			return;
//		}
//		if(object==null){
//			box.setInternetOffMessage(getResources().getString(R.string.getdata_Failure));
//			box.showInternetOffLayout();
//			return;
//		}
//		dataJson=ResolveUnit.resolve(object, MineQrcodeJson.class);
//		if(dataJson.getSuccess().equals(GlobleConfig.STATE_SUCCESS)){
//			BBLog.println(this, "获取我的二维码信息回调成功");
//			setView();
//			
//		}else{
//			box.setInternetOffMessage(dataJson.getMessage());
//			box.showInternetOffLayout();
//			actionToCheckLoginValid(dataJson.getMessage());
//		}
//		
//	}

}
