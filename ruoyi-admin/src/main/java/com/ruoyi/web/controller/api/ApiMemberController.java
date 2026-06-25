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
            return AjaxResult.error("Member not found");
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
            return AjaxResult.success("Password updated");
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
                return AjaxResult.error("Nickname must be 1-50 characters");
            }
            member.setNickName(nick.trim());
        }
        if (body.containsKey("phone"))
        {
            String phone = body.get("phone");
            if (phone != null && phone.length() > 20)
            {
                return AjaxResult.error("Phone number is too long");
            }
            member.setPhone(phone);
        }
        if (body.containsKey("avatarUrl"))
        {
            String avatar = body.get("avatarUrl");
            if (avatar != null && avatar.length() > 500)
            {
                return AjaxResult.error("Avatar URL is too long");
            }
            member.setAvatarUrl(avatar);
        }
        if (body.containsKey("gender"))
        {
            String gender = body.get("gender");
            if (gender != null && !gender.isEmpty()
                    && !"0".equals(gender) && !"1".equals(gender) && !"2".equals(gender))
            {
                return AjaxResult.error("Gender must be 0 (unknown), 1 (male) or 2 (female)");
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
                        return AjaxResult.error("Birthday cannot be in the future");
                    }
                    member.setBirthday(date);
                }
                catch (ParseException e)
                {
                    return AjaxResult.error("Birthday must be in yyyy-MM-dd format");
                }
            }
        }
        memberService.updateMember(member);
        return AjaxResult.success("Profile updated");
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
        return AjaxResult.success("Address added").put("data", address);
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
            return AjaxResult.error("Address not found");
        }
        address.setAddressId(id);
        address.setMemberId(memberId);
        addressService.updateAddress(address);
        return AjaxResult.success("Address updated");
    }

    /** 删除地址 */
    @DeleteMapping("/addresses/{id}")
    public AjaxResult deleteAddress(@PathVariable Long id, HttpServletRequest request)
    {
        Long memberId = getCurrentMemberId(request);
        MallAddress existing = addressService.selectAddressById(id);
        if (existing == null || !existing.getMemberId().equals(memberId))
        {
            return AjaxResult.error("Address not found");
        }
        addressService.deleteAddressById(id);
        return AjaxResult.success("Address deleted");
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
            return AjaxResult.error("fcmToken is required");
        }
        memberMapper.updateFcmToken(memberId, token);
        return AjaxResult.success("FCM token updated");
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
            return AjaxResult.error("Address not found");
        }
        addressService.setDefaultAddress(id, memberId);
        return AjaxResult.success("Default address updated");
    }
}
