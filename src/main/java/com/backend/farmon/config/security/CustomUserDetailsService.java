package com.backend.farmon.config.security;

import com.amazonaws.services.kms.model.DisabledException;
import com.backend.farmon.apiPayload.code.status.ErrorStatus;
import com.backend.farmon.apiPayload.exception.handler.UserHandler;
import com.backend.farmon.domain.User;
import com.backend.farmon.domain.enums.MemberStatus;
import com.backend.farmon.dto.user.CustomUserDetails;
import com.backend.farmon.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // DB에서 유저 정보 찾아내서 UserDetail생성하는 메서드
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 해당 이메일과 일치하는 유저 없을시 에러 발생
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorStatus.EMAIL_ALREADY_EXIST));

        // 회원상태 체크 (회원탈퇴한 사용자면 에러발생)
        if (user.getStatus() == MemberStatus.INACTIVE) {
            throw new DisabledException("회원탈퇴한 회원입니다.");
        }

        return new CustomUserDetails(user);
    }
}