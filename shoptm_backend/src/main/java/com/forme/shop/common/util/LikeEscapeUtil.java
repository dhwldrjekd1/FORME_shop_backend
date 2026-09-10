package com.forme.shop.common.util;

// LIKE 검색어에 와일드카드 문자(%, _)를 그대로 넘기면 진짜 문자가 아니라 패턴으로 해석돼
// 검색 결과가 부정확해진다(예: 이름에 '_'가 그대로 들어간 "a_b"를 검색했는데 "axb" 같은
// 무관한 결과까지 걸림). 회원/게시판/상품 검색에서 각자 같은 이스케이프 로직을 따로 복사해
// 쓰고 있었는데, 다음에 검색 기능이 하나 더 생기면 또 그대로 복사되다가 이스케이프 자체를
// 빠뜨리기 쉬워서 공용 유틸로 뺐다. 호출하는 쪽 리포지토리 쿼리는 반드시
// "LIKE CONCAT('%', :keyword, '%') ESCAPE '\\'" 형태로 이 이스케이프 문자(\)를 해석해야 한다.
public final class LikeEscapeUtil {

    private LikeEscapeUtil() {
    }

    public static String escape(String keyword) {
        if (keyword == null) return null;
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
