package com.callke8.pridialqueueforbshbyquartz;

import java.util.List;
import java.util.TimerTask;

import com.callke8.bsh.bshorderlist.BSHHttpRequestThread;
import com.callke8.bsh.bshorderlist.BSHOrderList;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.StringUtil;

/**
 * 强制清理超时订单数据的 TASK
 * 
 * BSH每天限定 09:00-20:00 为有效的呼叫时间
 * 
 * 如果状态仍为：0（新建）、3（待重呼）时，而安装日期是小于或是等于当前日期时，将状态修改为放弃外呼
 * 
 * 该程序在每天凌晨2点钟执行一次,每隔24小时执行一次
 * 
 * @author 黄文周
 */
public class BSHCleanTimeOutTask extends TimerTask {

	public BSHCleanTimeOutTask() {
		
	}
	
	@Override
	public void run() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("BSH处理 TIMEOUT 数据的线程准备执行!");
		sb.append("若订单状态为:0(新建),或为:3(待重呼),但是安装日期却小于等是等于当前日期时,系统却强制处理该记录,将状态修改为放弃呼叫!");
		
		//处理超时数据
		List<BSHOrderList> timeOutOrderList = BSHOrderList.dao.handleTimeOutOrderList();
		
		int timeOutOrderListCount = 0;
		
		if(!BlankUtils.isBlank(timeOutOrderList)) {
			
			timeOutOrderListCount = timeOutOrderList.size();
			
			for(BSHOrderList bshOrderList:timeOutOrderList) {    //遍历，用于将结果反馈给BSH服务器
				
				//在返回外呼结果给DOB服务器时，还需要加入一个前置流程的外呼结果
				//前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
				//由于这里外呼失败，所以前置外呼结果只能是： 0 （没有前置）或是3（未接听）
				String preCallResult = "0";
				int isConfirm = bshOrderList.getInt("IS_CONFIRM");
				int productName = bshOrderList.getInt("PRODUCT_NAME");
				if(isConfirm==1 && (productName==6 || productName==8)) {
					//如果 isConfirm==1 且 产品类目为 6（灶具）或是 8（洗碗机）时，表示有前置流程
					preCallResult = "3";
				}
				
				/**
				 * 取出外呼类型，1：确认安装；2：零售核实
				 */
				int outboundType = bshOrderList.getInt("OUTBOUND_TYPE");
				
				//对于超时的记录，将结果反馈给BSH服务器
				BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "2",preCallResult,"6",String.valueOf(outboundType));
				Thread httpRequestThread = new Thread(httpRequestT);
				httpRequestThread.start();
				
			}
		}
		
		sb.append("此次系统共处理 " + timeOutOrderListCount + " 条超时数据!");
		
		StringUtil.log(this, sb.toString());
		StringUtil.writeString("/data/bsh_exec_log/clean_timeout.log", DateFormatUtils.getCurrentDate() + "\t" + sb.toString() + "\r\n", true);
	}
	
}
