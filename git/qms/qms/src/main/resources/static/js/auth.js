// 페이지가 로드될 때 호출되는 함수
function initJwtCheck(redirectPath) {
    console.log("🚀 [디버깅] JWT 확인 시작");
    const token = sessionStorage.getItem("jwtToken");
    console.log("🔍 [디버깅] 저장된 JWT 값:", token);

    // JWT가 없다면 로그인 페이지로 이동
    if (!token) {
        console.warn("⚠️ [경고] JWT가 저장되지 않았습니다!");
        sessionStorage.removeItem("jwtToken");
        window.location.href = "/login";  // 로그인 페이지로 이동
    } else {
        console.log("✅ [디버깅] JWT 존재, 만료 여부 검사 시작");
        const expired = isJwtExpired(token);
        console.log("⏳ [디버깅] JWT 만료 여부:", expired);
        
        // JWT가 만료되지 않았다면 정상 처리
        if (expired) {
            console.warn("⚠️ [경고] JWT가 만료됨 → 로그인 페이지로 이동");
            sessionStorage.removeItem("jwtToken");
			alert("세션 만료됨 → 로그인 페이지로 이동");
            window.location.href = "/login";
        } else {
            console.log("✅ JWT가 유효함 → 정상적으로 진행");
            sessionStorage.setItem("jwtToken", token);
            // URL 상태 업데이트
            window.history.replaceState({}, document.title, redirectPath);
        }
    }
}
function isJwtExpired(token) {
    console.log("🚀 [디버깅] isJwtExpired() 함수 실행됨");
    try {
        const parts = token.split(".");
        if (parts.length !== 3) {
            throw new Error("JWT 형식이 올바르지 않습니다.");
        }
        const base64Url = parts[1];
        let base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        while (base64.length % 4 !== 0) {
            base64 += "=";
        }
        
        const decoded = atob(base64);
        const jsonPayload = JSON.parse(decoded);
        const exp = jsonPayload.exp; // 만료 시간
        
        const now = Math.floor(Date.now() / 1000); // 현재 시간
        console.log("✅ JWT가 남은 시간: ", exp - now, "초");
     	// 만료 체크
        if (now > exp) {
            return true; // 만료됨
        } else if (exp - now < 300) { // 5분 이내 만료될 경우
            refreshToken(token);
        }
        return false; // 유효함
        
    } catch (e) {
        console.error("❌ [오류] JWT 확인 중 오류 발생:", e.message);
        return true; // 오류 발생 시 만료된 것으로 간주
    }
}

function refreshToken(token) {
    console.log("🔄 [디버깅] JWT 갱신 요청 시작");
    $.ajax({
        url: '/refresh-token', // JWT 갱신 요청 엔드포인트
        type: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token // 현재 JWT를 Authorization 헤더에 포함
        },
        success: function(response) {
            if (response.token) {
                sessionStorage.setItem("jwtToken", response.token); // 새로운 토큰 저장
                console.log("✅ [디버깅] 새로운 JWT 저장:", response.token);
                // 여기에 추가적인 처리 (예: 페이지 내용 로드 등)
			}
			else if (response.sessionOut) { // 세션 아웃인 경우
                sessionStorage.removeItem("jwtToken"); // 토큰 제거
				alert("⚠️ 세션이 만료되었습니다. 다시 로그인 해주십시오.");
				window.location.href = "/login"; // 로그인 페이지로 이동	
            } else {
                console.warn("⚠️ [경고] JWT 갱신 실패, 로그인 페이지로 이동");
                sessionStorage.removeItem("jwtToken");
				alert("페이지 로드 중 오류가 발생했습니다.");
                window.location.href = "/login"; // 재갱신 실패 시 로그인 페이지로 이동
            }
        },
        error: function() {
            console.error("❌ [오류] JWT 갱신 요청 실패");
            sessionStorage.removeItem("jwtToken");
			alert("페이지 로드 중 오류가 발생했습니다.");
            window.location.href = "/login"; // 오류 발생 시 로그인 페이지로 이동
        }
    });
}
// 로그아웃 함수
function logout() {
    // 로그아웃 요청 보내기
    fetch('/logout', { method: 'GET' })
        .then(response => {
            if (response.redirected) {
                // 서버가 리다이렉트한 경우
                window.location.href = response.url; // 리다이렉트 URL로 이동
            } else if (response.ok) {
                // 성공적으로 로그아웃되면 세션 스토리지에서 JWT 토큰 제거
                sessionStorage.removeItem("jwtToken");
                console.log("✅ 로그아웃 성공, jwtToken 세션 삭제");
                // 로그인 페이지로 리다이렉트
                window.location.href = '/login';
                // 강제로 새로 고침
                window.location.reload(true);
            } else {
                console.warn("⚠️ 로그아웃 중 오류 발생");
            }
        })
        .catch(error => {
            console.error("❌ [오류] 로그아웃 요청 실패:", error);
        });
}

// 페이지 로드 시 JWT 확인 함수 호출
$(document).ready(function() {
    // 현재 페이지의 URL 경로를 전달합니다.
    initJwtCheck(window.location.pathname);
});


