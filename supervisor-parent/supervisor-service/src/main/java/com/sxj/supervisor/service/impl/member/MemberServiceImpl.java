package com.sxj.supervisor.service.impl.member;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sxj.supervisor.dao.member.IMemberDao;
import com.sxj.supervisor.entity.member.MemberEntity;
import com.sxj.supervisor.enu.member.MemberCheckStateEnum;
import com.sxj.supervisor.enu.member.MemberStatesEnum;
import com.sxj.supervisor.model.member.MemberQuery;
import com.sxj.supervisor.service.member.IMemberService;
import com.sxj.util.exception.ServiceException;
import com.sxj.util.persistent.QueryCondition;

@Service
@Transactional
public class MemberServiceImpl implements IMemberService {

	@Autowired
	private IMemberDao menberDao;

	/**
	 * 新增会员
	 */
	@Override
	public void addMember(MemberEntity member) {
		menberDao.addMember(member);
	}

	/**
	 * 更新会员
	 */
	@Override
	public void modifyMember(MemberEntity member) {
		MemberEntity mb = menberDao.getMember(member.getId());
		mb.setName(member.getName());
		mb.setbLicenseNo(member.getbLicenseNo());
		mb.setEnergyNo(member.getEnergyNo());
		mb.setContacts(member.getContacts());
		mb.setType(member.getType());
		mb.setPhoneNo(member.getPhoneNo());
		mb.setAddress(member.getAddress());
		mb.setTelNum(member.getTelNum());
		menberDao.updateMember(mb);
	}

	/**
	 * 查找会员
	 */
	@Override
	public MemberEntity getMember(String id) {
		MemberEntity member = menberDao.getMember(id);
		return member;
	}

	/**
	 * 会员高级查询
	 */
	@Override
	@Transactional(readOnly = true)
	public List<MemberEntity> queryMembers(MemberQuery query)
			throws ServiceException {
		try {
			if (query == null) {
				return null;
			}
			QueryCondition<MemberEntity> condition = new QueryCondition<MemberEntity>();
			if (query != null) {
				condition.addCondition("memberNo", query.getMemberNo());// 会员号
				condition.addCondition("name", query.getMemberName());// 会员名称
				condition.addCondition("contacts", query.getContacts());// 联系人名称
				condition.addCondition("phoneNo", query.getContactsPhone());// 联系人电话
				condition.addCondition("area", query.getArea());// 地理区域
				condition.addCondition("bLicenseNo", query.getbLicenseNo());// 营业执照号
				condition.addCondition("energyNo", query.getEnergyNo());// 节能标识号
				condition.addCondition("type", query.getMemberType());// 会员类型
				if (query.getCheckState() != null && query.getCheckState() == 3) {
					condition.addCondition("state", 1);// 状态
				} else {
					condition.addCondition("checkState", query.getCheckState());
				}
				condition.addCondition("startDate", query.getStartDate());// 开始时间
				condition.addCondition("endDate", query.getEndDate());// 结束时间
				condition.setPage(query);
			}
			List<MemberEntity> memberList = menberDao.queryMembers(condition);
			query.setPage(condition);
			return memberList;
		} catch (Exception e) {
			throw new ServiceException("查询会员信息错误", e);
		}

	}

	/**
	 * 删除会员
	 */
	@Override
	public void removeMember(String id) {
		menberDao.deleteMember(id);

	}

	@Override
	@Transactional
	public String initializePwd(String memberId) throws ServiceException {
		try {
			MemberEntity member = getMember(memberId);
			// 随机密码
			String password = "123456";
			member.setPassword(password);
			modifyMember(member);
			return password;
		} catch (Exception e) {
			throw new ServiceException("初始化密码错误", e.getMessage());
		}

	}

	/**
	 * 更改账户状态
	 */
	@Override
	public String editState(String id) {
		MemberEntity member = menberDao.getMember(id);
		if (member.getState().getId().intValue() == MemberStatesEnum.normal
				.getId().intValue()) {
			member.setState(MemberStatesEnum.stop);
			menberDao.updateMember(member);
			return MemberStatesEnum.stop.getName();
		} else {
			member.setState(MemberStatesEnum.normal);
			menberDao.updateMember(member);
			;
			return MemberStatesEnum.normal.getName();
		}
	}

	/**
	 * 更改审核状态
	 */
	@Override
	public String editCheckState(String id) {
		MemberEntity member = menberDao.getMember(id);
		if (member.getCheckState().getId().intValue() == MemberCheckStateEnum.unaudited
				.getId().intValue()) {
			member.setCheckState(MemberCheckStateEnum.unrecognized);
			menberDao.updateMember(member);
			return MemberCheckStateEnum.unrecognized.getName();
		} else if (member.getCheckState().getId().intValue() == MemberCheckStateEnum.unrecognized
				.getId().intValue()) {
			member.setCheckState(MemberCheckStateEnum.certified);
			menberDao.updateMember(member);
			return MemberCheckStateEnum.certified.getName();
		}
		return MemberCheckStateEnum.certified.getName();
	}

}
