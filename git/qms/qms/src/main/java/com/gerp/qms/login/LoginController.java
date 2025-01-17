package com.gerp.qms.login;

import com.gerp.qms.domain.Sys_user;
//import com.gerp.qms.LoginDAO; 같은 패키지로 옮김 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Controller
public class LoginController {
	String ls_comid = "DDMVS";

	@Autowired
	private LoginDAO dao;

	@GetMapping("/login")
	public ResponseModel getIndex(Model model, HttpSession session, HttpServletResponse response) {
		// 캐시를 비활성화하여 이전 페이지가 캐시되지 않도록 설정
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		
		System.out.println("controller /login");
		// 로그인 여부 확인
		if (session.getAttribute("loginTime") != null) {
			//return "redirect:/home"; // 로그인 상태이면 메인 페이지로 리다이렉트
			return new ResponseModel(null, false, false, false, false);
		}
		//return "/login"; // 로그인 페이지 반환
		return new ResponseModel(null, false, false, false, true);
	}

	@PostMapping(value = "/search")
	@ResponseBody
	public ResponseModel search(@RequestParam String userid, @RequestParam String userpw, Model model, HttpSession session, HttpServletResponse response) {
		System.out.println("controller /search");
		System.out.println(userid);
		System.out.println(userpw);
		List<Sys_user> sysuser = dao.Get_sysuser(ls_comid, userid, userpw); // 예시로 "ddmvs"를 전달
		model.addAttribute("sys_user", sysuser);

		if (sysuser.isEmpty()) {
			// 로그인 실패: userid와 userpw에 해당하는 데이터가 없음
			model.addAttribute("loginFail", true);
			System.out.println("로그인실패, 아이디/패스워드 틀림");
			// return "redirect:/login?loginFail=true";
			//return "/login";
			return new ResponseModel(null, true, false, false, false);
			
		} else {
			// 로그인 성공: sysuser에서 첫 번째 데이터 가져옴
			Sys_user loginData = sysuser.get(0);
			int vndmst_chk = dao.Get_vndmst_chk(ls_comid, loginData.getCvcod()); // 거래처 상태 체크
			if (vndmst_chk == 1) {
				// 세션에 사용자 정보 저장
				session.setAttribute("gs_comid", ls_comid);
				session.setAttribute("gs_deptcd", loginData.getDeptcd());
				session.setAttribute("gs_userid", loginData.getUserid());
				session.setAttribute("gs_userpw", loginData.getUserpw());
				session.setAttribute("gs_usernm", loginData.getUsernm());
				session.setAttribute("gs_empno",  loginData.getEmpno());
				session.setAttribute("gs_lancd",  loginData.getLancd());
				session.setAttribute("gs_cvcod",  loginData.getCvcod());
				session.setAttribute("loginTime", System.currentTimeMillis()); // 로그인 시간

				// 세션 저장 로그 출력
				System.out.println("로그인성공");
				System.out.println("세션 저장: userid=" + loginData.getUserid() + ", username=" + loginData.getUsernm() + ", deptcd=" + loginData.getDeptcd());
						
				//return "redirect:/home"; // 리다이렉트
				return new ResponseModel(null, false, false, false, false);
			} else {
				// 로그인 실패: 거래처 사용중지
				//model.addAttribute("loginFail_vndmst", true);
				System.out.println("로그인실패, 거래처 거래중지 상태 입니다");
				//return "/login";
				return new ResponseModel(null, false, true, false, false);
			}
		}

	}

	@GetMapping("/home")
	public String home(HttpSession session, Model model, HttpServletResponse response) {
		// 캐시를 비활성화하여 이전 페이지가 캐시되지 않도록 설정
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);

		// 세션에서 값 가져오기
		String userid  = (String) session.getAttribute("gs_userid");
		String usernm  = (String) session.getAttribute("gs_usernm");
		Long loginTime = (Long)   session.getAttribute("loginTime");

		// 세션 값 로그 출력
		System.out.println("home 페이지");
		System.out.println("세션 조회: comid =" + ls_comid + ", userid=" + userid + ", username=" + usernm + ", loginTime="
				+ loginTime);

		if ((userid == null || userid == "") && (usernm == null || usernm == "")) {
			// 세션 값이 없으면 로그인 페이지로 리다이렉트
			System.out.println("세션값 널 체크");
			model.addAttribute("SessionOut", true);
			return "redirect:/login?SessionOut=true";
		} else {
			// 세션 값 모델에 추가하여 뷰에서 사용
			List<Sys_user> syscom = dao.Get_syscom(ls_comid, usernm); // 예시로 "ddmvs"를 전달
			model.addAttribute("sys_com", syscom);
			model.addAttribute("usernm", usernm); // 모델에 추가
			System.out.println("/home return 부분");
			System.out.println("세션 조회: userid=" + userid + ", username= " + usernm);

			// model.addAttribute("userid", userid);
			return "/home"; // POST 후 리다이렉트
		}
	}

	@GetMapping("/logout")
	public String logout(HttpSession session, HttpServletResponse response) {
		System.out.println("세션무효화 logout");

		// Java 서블릿 또는 Spring Controller에서 다음과 같이 캐시 방지 헤더를 추가
		response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);

		// 세션 무효화
		session.invalidate();
		// 로그아웃 후 리다이렉트 (캐시를 방지한 후 로그인 페이지로 이동)
		return "redirect:/login"; // 리다이렉트
	}

	@GetMapping("/{path}")
	public String handleInvalidPath(@PathVariable String path, Model model) {
		// 잘못된 경로 처리
		model.addAttribute("message", "잘못된 페이지입니다: " + path);
		return "redirect:./error/404"; // 리다이렉트 error.jsp로 이동
	}

	@GetMapping("/error")
	public String errorPage() {
		return "redirect:./error/404"; // 리다이렉트 error.jsp로 이동
	}

	@GetMapping("/")
	public String homePage() {
		// 기본 페이지 처리
		return "redirect:/login"; // 리다이렉트eturn "/login";
	}
	
	//AJAX값으로 보냄 
	public static class ResponseModel {
		private List<Sys_user> sys_user;
		private boolean loginFail;
		private boolean loginFail_vndmst;
		private boolean SessionOut;
		private boolean initlgoin;

		public ResponseModel(List<Sys_user> sys_user, boolean loginFail, boolean loginFail_vndmst, boolean sessionOut, boolean initlgoin) {
			this.sys_user = sys_user;
			this.loginFail = loginFail;
			this.loginFail_vndmst = loginFail_vndmst;
			SessionOut = sessionOut;	
			this.initlgoin = initlgoin;
		}

		public List<Sys_user> getSys_user() {
			return sys_user;
		}

		public boolean isLoginFail() {
			return loginFail;
		}

		public boolean isLoginFail_vndmst() {
			return loginFail_vndmst;
		}

		public boolean isSessionOut() {
			return SessionOut;
		}
		public boolean isInitlgoin() {
			return initlgoin;
		}
	}
}