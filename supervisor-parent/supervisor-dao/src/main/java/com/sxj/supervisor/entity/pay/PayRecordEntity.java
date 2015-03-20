package com.sxj.supervisor.entity.pay;

import java.io.Serializable;
import java.util.Date;

import com.sxj.mybatis.orm.annotations.Column;
import com.sxj.mybatis.orm.annotations.Entity;
import com.sxj.mybatis.orm.annotations.GeneratedValue;
import com.sxj.mybatis.orm.annotations.GenerationType;
import com.sxj.mybatis.orm.annotations.Id;
import com.sxj.mybatis.orm.annotations.Sn;
import com.sxj.mybatis.orm.annotations.Table;
import com.sxj.supervisor.dao.contract.IContractPayDao;
import com.sxj.supervisor.enu.contract.PayContractTypeEnum;
import com.sxj.supervisor.enu.contract.PayModeEnum;
import com.sxj.supervisor.enu.contract.PayStageEnum;
import com.sxj.supervisor.enu.contract.PayTypeEnum;

/**
 * 支付单
 * 
 * @author dujinxin
 *
 */
@Entity(mapper = IContractPayDao.class)
@Table(name = "M_CONTRACT_PAY")
public class PayRecordEntity implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1316548893613242787L;

	/**
	 * ID
	 */
	@Id(column = "ID")
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	/**
	 * 甲方会员ID
	 */
	@Column(name = "MEMBER_NO_A")
	private String memberNoA;

	/**
	 * 甲方会员名称
	 */
	@Column(name = "MEMBER_NAME_A")
	private String memberNameA;

	/**
	 * 乙方会员ID
	 */
	@Column(name = "MEMBER_NO_B")
	private String memberNoB;

	/**
	 * 乙方会员名称
	 */
	@Column(name = "MEMBER_NAME_B")
	private String memberNameB;

	/**
	 * 支付单号
	 */
	@Column(name = "PAY_NO")
	@Sn(pattern = "00", step = 1, table = "T_SN", stub = "F_SN_NAME", sn = "F_SN_NUMBER", stubValueProperty = "dateNo")
	private String payNo;

	private String dateNo;

	/**
	 * 合同号
	 */
	@Column(name = "CONTRACT_NO")
	private String contractNo;

	/**
	 * 批次号
	 */
	@Column(name = "BATCH_NO")
	private String batchNo;

	/**
	 * RFID编号
	 */
	@Column(name = "RFID_NO")
	private String rfidNo;

	/**
	 * 支付金额
	 */
	@Column(name = "PAY_AMOUNT")
	private Double payAmount;

	/**
	 * 实际支付金额
	 */
	@Column(name = "PAY_REAL")
	private Double payReal;

	/**
	 * 支付日期
	 */
	@Column(name = "PAY_DATE")
	private Date payDate;

	/**
	 * 支付内容
	 */
	@Column(name = "CONTENT")
	private String content;

	/**
	 * 状态
	 */
	@Column(name = "STATE")
	private PayStageEnum state;

	/**
	 * 支付内容状态
	 * 
	 * @return
	 */
	@Column(name = "PAY_TYPE")
	private PayTypeEnum payType;

	/**
	 * 类型
	 * 
	 * @return
	 */
	@Column(name = "PAY_CONTRACT_TYPE")
	private PayContractTypeEnum contractType;

	/**
	 * 付款方式
	 * 
	 * @return
	 */
	@Column(name = "PAY_MODE")
	private PayModeEnum payMode;

	/**
	 * 付款方式
	 * 
	 * @return
	 */
	@Column(name = "CREAT_PAY_DATE")
	private Date creatPayDate;

	public String getContent() {
		return content;
	}

	public Double getPayReal() {
		return payReal;
	}

	public void setPayReal(Double payReal) {
		this.payReal = payReal;
	}

	public PayModeEnum getPayMode() {
		return payMode;
	}

	public void setPayMode(PayModeEnum payMode) {
		this.payMode = payMode;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getMemberNoA() {
		return memberNoA;
	}

	public void setMemberNoA(String memberNoA) {
		this.memberNoA = memberNoA;
	}

	public String getMemberNoB() {
		return memberNoB;
	}

	public void setMemberNoB(String memberNoB) {
		this.memberNoB = memberNoB;
	}

	public String getId() {
		return id;
	}

	public String getDateNo() {
		return dateNo;
	}

	public void setDateNo(String dateNo) {
		this.dateNo = dateNo;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPayNo() {
		return payNo;
	}

	public void setPayNo(String payNo) {
		this.payNo = payNo;
	}

	public String getContractNo() {
		return contractNo;
	}

	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}

	public String getBatchNo() {
		return batchNo;
	}

	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
	}

	public String getRfidNo() {
		return rfidNo;
	}

	public void setRfidNo(String rfidNo) {
		this.rfidNo = rfidNo;
	}

	public Double getPayAmount() {
		return payAmount;
	}

	public void setPayAmount(Double payAmount) {
		this.payAmount = payAmount;
	}

	public Date getPayDate() {
		return payDate;
	}

	public void setPayDate(Date payDate) {
		this.payDate = payDate;
	}

	public PayStageEnum getState() {
		return state;
	}

	public void setState(PayStageEnum state) {
		this.state = state;
	}

	public String getMemberNameA() {
		return memberNameA;
	}

	public void setMemberNameA(String memberNameA) {
		this.memberNameA = memberNameA;
	}

	public String getMemberNameB() {
		return memberNameB;
	}

	public void setMemberNameB(String memberNameB) {
		this.memberNameB = memberNameB;
	}

	public PayTypeEnum getPayType() {
		return payType;
	}

	public void setPayType(PayTypeEnum payType) {
		this.payType = payType;
	}

	public PayContractTypeEnum getContractType() {
		return contractType;
	}

	public void setContractType(PayContractTypeEnum contractType) {
		this.contractType = contractType;
	}

	public Date getCreatPayDate() {
		return creatPayDate;
	}

	public void setCreatPayDate(Date creatPayDate) {
		this.creatPayDate = creatPayDate;
	}

}
