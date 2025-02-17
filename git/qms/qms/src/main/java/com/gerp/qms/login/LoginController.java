package com.gerp.qms.login;

//import com.gerp.qms.LoginDAO; 같은 패키지로 옮김 
import com.gerp.qms.domain.Sys_user;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.ExpiredJwtException;

@Controller
public class LoginController {
	String ls_comid = "DDMVS";

	@Autowired
	private LoginDAO dao;
	
	 // JwtService 인스턴스 추가
    private final JwtService jwtService = new JwtService();

	@GetMapping("/login")
	//@PostMapping("/login")
	//@RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
	public ResponseModel handleLogin(Model model, HttpSession session, HttpServletResponse response) {
		// 캐시를 비활성화하여 이전 페이지가 캐시되지 않도록 설정
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
		System.out.println("■■■ controller /login 시작화면");
		//return "/login"; // 로그인 페이지 반환
		return new ResponseModel(false, false, false, true, null);
	}
	
	@PostMapping(value = "/search", produces = "application/json")
	@ResponseBody	
	//public ResponseModel search(@RequestParam String userid, @RequestParam String userpw, Model model, HttpSession session, HttpServletResponse response) {
	public ResponseModel search(@RequestBody LoginRequest request, Model model, HttpSession session, HttpServletResponse response) {
	    System.out.println("🔹 controller /search 호출됨");
	    System.out.println("🔹 HTTP 요청 방식: " + request.getClass().getSimpleName());
	    System.out.println("🔹 userid: " + request.getUserid());
	    System.out.println("🔹 userpw: " + request.getUserpw());
		
	    if (request.getUserid() == null || request.getUserid().trim().isEmpty()) {
	        System.out.println("❌ userid가 비어있음!");
	        return new ResponseModel(true, false, false, false, null);
	    }
	    
		List<Sys_user> sysuser = dao.Get_sysuser(ls_comid, request.getUserid(), request.getUserpw()); // 예시로 "ddmvs"를 전달
		model.addAttribute("sys_user", sysuser);

		if (sysuser.isEmpty()) {
			// 로그인 실패: userid와 userpw에 해당하는 데이터가 없음
			//model.addAttribute("loginFail", true);
		    System.out.println("❌ 로그인 실패: 아이디/패스워드 틀림");
			// return "redirect:/login?loginFail=true";
			//return "/login";
			return new ResponseModel(true, false, false, false, null);
			
		} else {
			// 로그인 성공: sysuser에서 첫 번째 데이터 가져옴
			Sys_user login = sysuser.get(0);
			int vndmst_chk = dao.Get_vndmst_chk(ls_comid, login.getCvcod()); // 거래처 상태 체크
			if (vndmst_chk == 1) {
				
				// JWT 생성 JwtService를 사용하여 토큰 생성
                String token = jwtService.generateToken(login.getUserid(), login.getDeptcd(), login.getUsernm(), login.getEmpno(), login.getLancd(), login.getCvcod()); 
                	
				// 세션 저장 로그 출력
				System.out.println("✅ 로그인 성공! 토큰생성 완료");
				System.out.println("토큰 저장: userid=" + login.getUserid() + ", username=" + login.getUsernm() + ", deptcd=" + login.getDeptcd());
				System.out.println("토큰 생선된값: " + token);
				
				//return "redirect:/main"; // 리다이렉트
				// 토큰을 URL 파라미터로 포함하여 리다이렉트
				// JWT를 세션에 저장
	            session.setAttribute("jwtToken", token);
				return new ResponseModel(false, false, false, false, token);
			} else {
				// 로그인 실패: 거래처 사용중지
				//model.addAttribute("loginFail_vndmst", true);
				System.out.println("-----------------");
				System.out.println("❌ 로그인실패, 거래처 거래중지 상태 입니다");
				//return "/login";
				return new ResponseModel(false, true, false, false, null);
			}
		}
	}

	@GetMapping("/main")
	public ResponseModel main(@RequestParam(value = "token", required = false) String token,
			           @RequestHeader(value = "Authorization", required = false) String authHeader, 
					   HttpSession session, Model model) {
		System.out.println("■■ main 페이지(@GetMapping(\"/main\"))");

		if (token == null || token.isEmpty()) {
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				token = authHeader.substring(7); // "Bearer " 이후의 토큰 추출
			}
	        else 
	        {
	        	 // 세션에서 토큰 가져오기
	            token = (String) session.getAttribute("jwtToken");
	        }
		}
		System.out.println("토큰 값: " + token);
		
	    // 토큰이 여전히 없으면 로그인 페이지로 리다이렉트
	    if (token == null || token.isEmpty()) {
	        System.out.println("토큰 없음 → login 리턴");
	        //return "redirect:/login";
	        return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
	    }

		// JWT에서 사용자 정보 추출
		Claims claims;
		try {
			claims = jwtService.decodeToken(token);
		} catch (SignatureException e) {
			System.out.println("/main컨트롤 -- JWT 서명 오류: " + e.getMessage());
			return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
		} catch (ExpiredJwtException e) {
			System.out.println("/main컨트롤 -- JWT 만료 오류: " + e.getMessage());
			return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
		} catch (Exception e) {
			System.out.println("/main컨트롤-- JWT 디코딩 실패: " + e.getMessage());
			return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
		}

		// ✅ JWT 정보 추출
		String gs_userid = claims.getSubject(); // 사용자 ID
		String gs_usernm = claims.get("gs_usernm", String.class); // 사용자 이름
		String gs_deptcd = claims.get("gs_deptcd", String.class); // 부서 코드

		if (gs_userid == null) {
			// 사용자 정보가 없으면 로그인 페이지로 리다이렉트
			model.addAttribute("sessionOut", true);
			//return "redirect:/login?sessionOut=true";
			return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
		} else {
			// 사용자 정보를 모델에 추가하여 뷰에서 사용
			System.out.println("로그인 SYSCOM조회");
			// 토큰값 로그 출력
			System.out.println(
					"토큰값 가져오기 성공, 조회: userid =" + gs_userid + ", usernm=" + gs_usernm + ", deptcd=" + gs_deptcd);
			if (gs_usernm == null) {
				gs_usernm = "";
			}
			List<Sys_user> syscom = dao.Get_syscom(ls_comid, gs_usernm);
			model.addAttribute("sys_com", syscom);
			model.addAttribute("usernm", gs_usernm); // 모델에 추가
			System.out.println("main 리다이렉트 userid =" + gs_userid + ", usernm=" + gs_usernm);
			//eturn "/main"; // 쿼리 파라미터로 토큰 전달
			return new ResponseModel(false, false, false, false, token); // 토큰 응답
		}
	}
	
	//JMT 토큰값 재갱신 
	@PostMapping(value = "/refresh-token", produces = "application/json")
	@ResponseBody
	public ResponseModel refreshToken(HttpSession session, @RequestHeader("Authorization") String authHeader) {
	    String token = null;

	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        token = authHeader.substring(7); // "Bearer " 이후의 토큰 추출
	    } else {
	        token = (String) session.getAttribute("jwtToken"); // 세션에서 토큰 가져오기
	    }

	    if (token == null) {
	        return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
	    }

	    try {
	        Claims claims = jwtService.decodeToken(token); // 현재 토큰의 유효성 확인
	        String newToken = jwtService.generateToken(claims.getSubject(), claims.get("gs_deptcd", String.class),
	                claims.get("gs_usernm", String.class), claims.get("gs_empno", String.class),
	                claims.get("gs_lancd", String.class), claims.get("gs_cvcod", String.class)); // 새로운 토큰 생성

	        session.setAttribute("jwtToken", newToken); // 세션에 새로운 토큰 저장
	        return new ResponseModel(false, false, false, false, newToken); // 새로운 토큰 응답
	    } catch (Exception e) {
	        System.out.println("JWT 갱신 실패: " + e.getMessage());
	        return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
	    }
	}
	
	@GetMapping("/logout")
	@ResponseBody // JSON 응답을 반환하기 위해 추가
	public ResponseModel logout(HttpSession session, HttpServletResponse response) {
		System.out.println("-----------------");
		System.out.println("세션무효화 logout");

		// Java 서블릿 또는 Spring Controller에서 다음과 같이 캐시 방지 헤더를 추가
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);

		// 세션에서 토큰 제거
	    session.removeAttribute("jwtToken");
	    
		// 세션 무효화
		session.invalidate();
		System.out.println("세션 무효화 완료");
		// 로그아웃 후 리다이렉트 (캐시를 방지한 후 로그인 페이지로 이동)
		//return "redirect:/login"; // 리다이렉트
		return new ResponseModel(false, false, true, false, null); // 토큰이 없으면 실패 응답(세션아웃)
	}

	@GetMapping("/{path}")
	public String handleInvalidPath(@PathVariable String path, Model model) {
		// 잘못된 경로 처리
		model.addAttribute("message", "잘못된 페이지입니다: " + path);
		//return "redirect:./error/404"; // 리다이렉트 error.jsp로 이동
		return "error/404"; // 리다이렉트가 아니라 해당 뷰를 직접 반환
	}

	@GetMapping("/error")
	public String errorPage() {
		//return "redirect:./error/404"; // 리다이렉트 error.jsp로 이동
		return "error/404"; // 리다이렉트가 아니라 해당 뷰를 직접 반환
	}

	@GetMapping("/")
	public String homePage() {
		// 기본 페이지 처리
		return "redirect:/login"; // 리다이렉트eturn "/login";
	}
	
	//AJAX값으로 보냄 
	public static class ResponseModel {
		@JsonProperty("sys_user")
		private List<Sys_user> sys_user;
		
		@JsonProperty("loginFail")
		private boolean loginFail;
		
		@JsonProperty("loginFail_vndmst")
		private boolean loginFail_vndmst;
		
		@JsonProperty("sessionOut")
		private boolean sessionOut;
		
		@JsonProperty("initLogin")
		private boolean initlgoin;
		
		@JsonProperty("token")
		private String token;

		public ResponseModel(boolean loginFail, boolean loginFail_vndmst, boolean sessionOut, boolean initlgoin, String token) {
			this.loginFail = loginFail;
			this.loginFail_vndmst = loginFail_vndmst;
			this.sessionOut = sessionOut;	
			this.initlgoin = initlgoin;
			this.token = token;
		}
		public boolean isLoginFail() {
			return loginFail;
		}

		public boolean isLoginFail_vndmst() {
			return loginFail_vndmst;
		}

		public boolean isSessionOut() {
			return sessionOut;
		}
		public boolean isInitlgoin() {
			return initlgoin;
		}
		public String getToken() {
	        return token;
	    }
	}
	// JSON 요청을 받을 DTO 클래스 추가
	public static class LoginRequest {
	    private String userid;
	    private String userpw;

	    public String getUserid() {
	        return userid;
	    }

	    public void setUserid(String userid) {
	        this.userid = userid;
	    }

	    public String getUserpw() {
	        return userpw;
	    }

	    public void setUserpw(String userpw) {
	        this.userpw = userpw;
	    }
	}
}