package com.gerp.qms.login;
import com.gerp.qms.domain.Sys_user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;


@Mapper
public interface LoginDAO {
	//1.sys_com 자료 조회
    @Select("SELECT COMID, CVNAS, ADDR, #{usernm} AS USERNM FROM SYS_COM WHERE COMID = #{comid} AND ROWNUM=1")
    List<Sys_user> Get_syscom(String comid, String usernm);
    
    //2.sys_userid, 기본거래처 정보 조회 및 아이디/비번 체크
    @Select(" SELECT USERID, USERNM, EMPNO, " +
                    "FUN_GET_EMPCARD_V(#{comid}, EMPNO, 'DEPTCD') AS DEPTCD, EMPNO AS CVCOD, " +
                    "(SELECT NATIONCD FROM SYS_COM WHERE COMID = #{comid} ) AS LANCD " +
            " FROM SYS_USER " +
            " WHERE COMID = #{comid} AND UPPER(USERID) = UPPER(#{userid}) AND UPPER(USERPW) = UPPER(#{userpw}) AND NVL(SYSTEMDIV, 'ERP')='QMS' ")

    List<Sys_user> Get_sysuser(String comid, String userid, String userpw);
    
    //3.ST_VNDMST 거래처 거래 여부 확인
    @Select(" SELECT COUNT(*) AS CNT " +
    		" FROM ST_VNDMST " +
    		" WHERE COMID = #{comid} AND CVCOD = #{cvcod} AND NVL(USEDIV, 'N')='Y' ")
    int Get_vndmst_chk(String comid,  String cvcod);
    
    /* 값 변환할때 확인
    int Get_sysuser(String comid,  String userid, String userpw);
    List<LoginDATA>: 여러 개의 결과 행을 반환하는 쿼리에 사용합니다.
    int: 단일 값을 반환하는 쿼리, 예를 들어 COUNT(*) 같은 집계 쿼리에서 사용합니다.
    Map<String, Object>: 다양한 데이터 타입을 반환하고자 할 때 사용합니다.
    Optional<LoginDATA>: 값이 있을 수도 있고 없을 수도 있는 경우, 예를 들어 하나의 결과를 찾을 때 사용합니다.
    */
}

