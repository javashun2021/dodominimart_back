package com.ruoyi.mall.service.impl;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.ruoyi.mall.domain.MallMember;
import com.ruoyi.mall.domain.MallVerifyCode;
import com.ruoyi.mall.mapper.MallMemberMapper;
import com.ruoyi.mall.mapper.MallVerifyCodeMapper;
import com.ruoyi.mall.service.IMallMemberService;
import com.ruoyi.mall.service.IMallCouponService;
import com.ruoyi.mall.service.IMallPointsService;

@Service
public class MallMemberServiceImpl implements IMallMemberService
{
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.]{2,}$");
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private MallMemberMapper memberMapper;

    @Autowired
    private MallVerifyCodeMapper verifyCodeMapper;

    @Autowired
    private IMallPointsService pointsService;

    @Autowired
    private IMallCouponService couponService;

    @Override
    public List<MallMember> selectMemberList(MallMember member)
    {
        return memberMapper.selectMemberList(member);
    }

    @Override
    public MallMember selectMemberById(Long memberId)
    {
        return memberMapper.selectMemberById(memberId);
    }

    @Override
    public MallMember loginOrRegisterByGoogle(String googleId, String email, String nickName, String avatarUrl, String referralCode)
    {
        MallMember member = memberMapper.selectMemberByGoogleId(googleId);
        if (member != null)
        {
            member.setNickName(nickName);
            member.setAvatarUrl(avatarUrl);
            member.setUpdateTime(new Date());
            memberMapper.updateMember(member);
            return member;
        }
        member = new MallMember();
        member.setGoogleId(googleId);
        member.setEmail(email);
        member.setNickName(nickName);
        member.setAvatarUrl(avatarUrl);
        member.setStatus("0");
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        onNewMemberCreated(member, referralCode);
        return memberMapper.selectMemberById(member.getMemberId());
    }

    @Override
    public MallMember loginOrRegisterByApple(String appleId, String email, String nickName, String referralCode)
    {
        MallMember member = memberMapper.selectMemberByAppleId(appleId);
        if (member != null)
        {
            return member;
        }
        member = new MallMember();
        member.setAppleId(appleId);
        member.setEmail(email != null ? email : "");
        member.setNickName(nickName != null ? nickName : "Apple User");
        member.setStatus("0");
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        onNewMemberCreated(member, referralCode);
        return memberMapper.selectMemberById(member.getMemberId());
    }

    @Override
    public String createVerifyCode(String email)
    {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches())
        {
            throw new RuntimeException("Invalid email format");
        }
        String normalizedEmail = email.trim();

        MallVerifyCode latest = verifyCodeMapper.selectLatestByEmail(normalizedEmail);
        if (latest != null)
        {
            long secondsSince = (System.currentTimeMillis() - latest.getCreateTime().getTime()) / 1000;
            if (secondsSince < 60)
            {
                throw new RuntimeException("Please wait 60 seconds before requesting another code");
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + 10 * 60 * 1000L);

        MallVerifyCode record = new MallVerifyCode();
        record.setEmail(normalizedEmail);
        record.setCode(code);
        record.setExpiresAt(expiresAt);
        record.setCreateTime(now);
        verifyCodeMapper.insert(record);

        return code;
    }

    @Override
    public MallMember registerByEmail(String email, String code, String password, String nickName, String referralCode)
    {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches())
        {
            throw new RuntimeException("Invalid email format");
        }
        if (password == null || password.length() < 8)
        {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (nickName == null || nickName.trim().isEmpty())
        {
            throw new RuntimeException("nickName is required");
        }

        String normalizedEmail = email.trim();

        MallVerifyCode record = verifyCodeMapper.selectLatestByEmail(normalizedEmail);
        if (record == null || record.getUsed() != 0 || record.getExpiresAt().before(new Date()))
        {
            throw new RuntimeException("Invalid or expired verification code");
        }
        if (record.getAttempts() != null && record.getAttempts() >= 5)
        {
            verifyCodeMapper.markUsed(record.getId());
            throw new RuntimeException("Too many attempts. Please request a new verification code");
        }
        if (!record.getCode().equals(code))
        {
            verifyCodeMapper.incrementAttempts(record.getId());
            throw new RuntimeException("Invalid or expired verification code");
        }
        verifyCodeMapper.markUsed(record.getId());

        MallMember existing = memberMapper.selectMemberByEmail(normalizedEmail);
        if (existing != null)
        {
            if (existing.getPasswordHash() != null)
            {
                throw new RuntimeException("Email already registered");
            }
            throw new RuntimeException("Email already linked to a social account. Please use Google or Apple Sign-In.");
        }

        MallMember member = new MallMember();
        member.setEmail(normalizedEmail);
        member.setNickName(nickName.trim());
        member.setAvatarUrl("");
        member.setStatus("0");
        member.setPasswordHash(BCRYPT.encode(password));
        member.setCreateTime(new Date());
        memberMapper.insertMember(member);
        onNewMemberCreated(member, referralCode);
        return memberMapper.selectMemberById(member.getMemberId());
    }

    @Override
    public MallMember loginByEmail(String email, String password)
    {
        final String ERR = "Invalid email or password";
        if (email == null || password == null)
        {
            throw new RuntimeException(ERR);
        }
        MallMember member = memberMapper.selectMemberByEmail(email.trim());
        if (member == null || member.getPasswordHash() == null)
        {
            throw new RuntimeException(ERR);
        }
        if (!BCRYPT.matches(password, member.getPasswordHash()))
        {
            throw new RuntimeException(ERR);
        }
        if (!"0".equals(member.getStatus()))
        {
            throw new RuntimeException("Account is disabled");
        }
        return member;
    }

    @Override
    public int updateMember(MallMember member)
    {
        member.setUpdateTime(new Date());
        return memberMapper.updateMember(member);
    }

    @Override
    public int updateRole(Long memberId, String role)
    {
        return memberMapper.updateRole(memberId, role);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private void onNewMemberCreated(MallMember member, String referralCode)
    {
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

        // 1. Generate and persist unique invite code
        String inviteCode = generateInviteCode(member.getMemberId());
        memberMapper.updateInviteCode(member.getMemberId(), inviteCode);

        // 2. Bind referrer if a valid referral code was provided
        boolean hasReferrer = false;
        if (referralCode != null && !referralCode.trim().isEmpty())
        {
            MallMember referrer = memberMapper.selectByInviteCode(referralCode.trim().toUpperCase());
            if (referrer != null && !referrer.getMemberId().equals(member.getMemberId()))
            {
                memberMapper.updateReferrerId(member.getMemberId(), referrer.getMemberId());
                hasReferrer = true;
            }
        }

        // 3. Welcome points:
        //    - No referral → 50 pts (1 record)
        //    - Via referral → 50 pts registration + 150 pts referral bonus (2 records, total 200)
        try
        {
            pointsService.earn(member.getMemberId(), 50, 3, null,
                    "Welcome bonus – registration gift");
            if (hasReferrer)
            {
                pointsService.earn(member.getMemberId(), 150, 5, null,
                        "Referral bonus – joined via invitation");
            }
        }
        catch (Exception e)
        {
            log.warn("Welcome points failed for member {}: {}", member.getMemberId(), e.getMessage());
        }

        // 4. Issue new-user coupons (Free Delivery + ₱30 Off + First Order 15% Off)
        try
        {
            couponService.issueNewUserCoupons(member.getMemberId());
        }
        catch (Exception e)
        {
            log.warn("Welcome coupons failed for member {}: {}", member.getMemberId(), e.getMessage());
        }
    }

    private String generateInviteCode(Long memberId)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((memberId + "dodo").getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            String candidate = sb.toString().substring(0, 6).toUpperCase();
            // Ensure uniqueness; retry with random suffix if collision
            for (int i = 0; i < 5; i++)
            {
                if (memberMapper.selectByInviteCode(candidate) == null) return candidate;
                candidate = sb.toString().substring(i + 1, i + 7).toUpperCase();
            }
            // Fallback: random 8 chars
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            StringBuilder fallback = new StringBuilder(8);
            for (int i = 0; i < 8; i++) fallback.append(chars.charAt(RANDOM.nextInt(chars.length())));
            return fallback.toString();
        }
        catch (Exception e)
        {
            return String.valueOf(memberId + System.currentTimeMillis()).substring(0, 6).toUpperCase();
        }
    }
}
