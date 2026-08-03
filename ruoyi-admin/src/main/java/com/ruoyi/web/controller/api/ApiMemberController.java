package com.ruoyi.web.controller.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.base.AjaxResult;
import com.ruoyi.mall.domain.MallAddress;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.service.IMallAddressService;
import com.ruoyi.mall.service.IMallMemberService;

/**
 * 会员资料和地址接口（需 JWT）
 * GET  /api/v1/member/profile
 * PUT  /api/v1/member/profile
 * GET  /api/v1/member/addresses
 * POST /api/v1/member/addresses
 * PUT  /api/v1/member/addresses/{id}
 * DELETE /api/v1/member/addresses/{id}
 */
@RestController
@RequestMapping("/api/v1/member")
public class ApiMemberController extends BaseApiController
{
    @Autowired
    private IMallMemberService memberService;

    @Autowired
    private IMallAddressService addressService;

    @Autowired
    private MallMemberMapper memberMapper;

    /** 获取个人信息 */
    @GetMapping("/profile")
    public AjaxResult getProfile(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallMember member = memberService.selectMemberById(memberId);
        if (member == null)
        {
            return AjaxResult.error("会员不存在");
        }
        // 标记是否已设置登录密码（供前端显示“设置/修改密码”），再清除哈希
        member.setHasPassword(member.getPasswordHash() != null);
        member.setPasswordHash(null);
        return AjaxResult.success("ok").put("data", member);
    }

    /**
     * 设置 / 修改登录密码
     * PUT /api/v1/member/password
     * Body: { "oldPassword": "...", "newPassword": "..." }
     * Google/Apple 登录用户首次设置时可不传 oldPassword。
     */
    @PutMapping("/password")
    public AjaxResult updatePassword(@RequestBody Map<String, String> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        try
        {
            memberService.setPassword(memberId, oldPassword, newPassword);
            return AjaxResult.success("密码修改成功");
        }
        catch (RuntimeException e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 更新个人信息（可改 nickName / phone / avatarUrl / gender / birthday，均为可选）
     * Body: { "nickName": "...", "phone": "...", "avatarUrl": "...", "gender": "1", "birthday": "1995-08-20" }
     */
    @PutMapping("/profile")
    public AjaxResult updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallMember member = new MallMember();
        member.setMemberId(memberId);
        if (body.containsKey("nickName"))
        {
            String nick = body.get("nickName");
            if (nick == null || nick.trim().isEmpty() || nick.length() > 50)
            {
                return AjaxResult.error("昵称需为 1-50 个字符");
            }
            member.setNickName(nick.trim());
        }
        if (body.containsKey("phone"))
        {
            String phone = body.get("phone");
            if (phone != null && phone.length() > 20)
            {
                return AjaxResult.error("手机号过长");
            }
            member.setPhone(phone);
        }
        if (body.containsKey("avatarUrl"))
        {
            String avatar = body.get("avatarUrl");
            if (avatar != null && avatar.length() > 500)
            {
                return AjaxResult.error("头像地址过长");
            }
            member.setAvatarUrl(avatar);
        }
        if (body.containsKey("gender"))
        {
            String gender = body.get("gender");
            if (gender != null && !gender.isEmpty()
                    && !"0".equals(gender) && !"1".equals(gender) && !"2".equals(gender))
            {
                return AjaxResult.error("性别只能是 0（未知）、1（男）或 2（女）");
            }
            member.setGender((gender == null || gender.isEmpty()) ? "0" : gender);
        }
        if (body.containsKey("birthday"))
        {
            String birthday = body.get("birthday");
            if (birthday != null && !birthday.trim().isEmpty())
            {
                try
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setLenient(false);
                    Date date = sdf.parse(birthday.trim());
                    if (date.after(new Date()))
                    {
                        return AjaxResult.error("生日不能是未来日期");
                    }
                    member.setBirthday(date);
                }
                catch (ParseException e)
                {
                    return AjaxResult.error("生日格式须为 yyyy-MM-dd");
                }
            }
        }
        memberService.updateMember(member);
        return AjaxResult.success("资料修改成功");
    }

    // ── 地址管理 ──────────────────────────────────────────

    /** 地址列表 */
    @GetMapping("/addresses")
    public AjaxResult listAddresses(HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        List<MallAddress> list = addressService.selectAddressByMemberId(memberId);
        return AjaxResult.success("ok").put("data", list);
    }

    /**
     * 新增地址
     * Body: { "label": "Home", "fullAddress": "...", "isDefault": "1" }
     */
    @PostMapping("/addresses")
    public AjaxResult addAddress(@RequestBody MallAddress address, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        address.setMemberId(memberId);
        if (address.getIsDefault() == null)
        {
            address.setIsDefault("0");
        }
        addressService.insertAddress(address);
        return AjaxResult.success("地址添加成功").put("data", address);
    }

    /**
     * 修改地址
     * Body: { "label": "...", "fullAddress": "...", "isDefault": "1" }
     */
    @PutMapping("/addresses/{id}")
    public AjaxResult updateAddress(@PathVariable Long id,
            @RequestBody MallAddress address, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallAddress existing = addressService.selectAddressById(id);
        if (existing == null || !existing.getMemberId().equals(memberId))
        {
            return AjaxResult.error("地址不存在");
        }
        address.setAddressId(id);
        address.setMemberId(memberId);
        addressService.updateAddress(address);
        return AjaxResult.success("地址修改成功");
    }

    /** 删除地址 */
    @DeleteMapping("/addresses/{id}")
    public AjaxResult deleteAddress(@PathVariable Long id, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallAddress existing = addressService.selectAddressById(id);
        if (existing == null || !existing.getMemberId().equals(memberId))
        {
            return AjaxResult.error("地址不存在");
        }
        addressService.deleteAddressById(id);
        return AjaxResult.success("地址删除成功");
    }

    /**
     * 上报 FCM 设备 token
     * PUT /api/v1/member/fcm-token
     * Body: { "fcmToken": "..." }
     */
    @PutMapping("/fcm-token")
    public AjaxResult updateFcmToken(@RequestBody Map<String, String> body, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        String token = body.get("fcmToken");
        if (token == null || token.isEmpty())
        {
            return AjaxResult.error("fcmToken 不能为空");
        }
        memberMapper.updateFcmToken(memberId, token);
        return AjaxResult.success("FCM Token 更新成功");
    }

    /**
     * 设置默认地址
     * PUT /api/v1/member/addresses/{id}/default
     */
    @PutMapping("/addresses/{id}/default")
    public AjaxResult setDefaultAddress(@PathVariable Long id, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallAddress existing = addressService.selectAddressById(id);
        if (existing == null || !existing.getMemberId().equals(memberId))
        {
            return AjaxResult.error("地址不存在");
        }
        addressService.setDefaultAddress(id, memberId);
        return AjaxResult.success("默认地址设置成功");
    }
}
