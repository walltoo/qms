package com.gerp.qms.login;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.Date;

public class JwtService {
    private final String secretKey;

    public JwtService() {
        KeyGenerator keyGenerator = new KeyGenerator(); // 비밀 키 생성기 인스턴스 생성
        this.secretKey = keyGenerator.getSecretKey(); // 비밀 키 가져오기
    }

    @SuppressWarnings("deprecation")
	public String generateToken(String gs_userid, String gs_deptcd, String gs_usernm, String gs_empno, String gs_lancd, String gs_cvcod) {
        return Jwts.builder()
                .setSubject(gs_userid) // 주제는 userid
                .claim("gs_deptcd", gs_deptcd)
                .claim("gs_usernm", gs_usernm)
                .claim("gs_empno", gs_empno)
                .claim("gs_lancd", gs_lancd)
                .claim("gs_cvcod", gs_cvcod)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1800000)) // 30분 후 만료 (30분 = 30 * 60 * 1000 밀리초)
                //.setExpiration(new Date(System.currentTimeMillis() + 60000)) //테스트 1분 
                .signWith(SignatureAlgorithm.HS256, secretKey) // 비밀 키 사용
                .compact();
    }
    
    @SuppressWarnings("deprecation")
    public Claims decodeToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            System.out.println("JWT 서명 오류: " + e.getMessage());
            throw new IllegalArgumentException("JWT 서명이 유효하지 않습니다.");
        } catch (ExpiredJwtException e) {
            System.out.println("JWT 만료 오류: " + e.getMessage());
            throw new IllegalArgumentException("JWT가 만료되었습니다.");
        } catch (Exception e) {
            System.out.println("JWT 디코딩 실패: " + e.getMessage());
            throw new IllegalArgumentException("JWT 디코딩에 실패했습니다.");
        }
    }
}