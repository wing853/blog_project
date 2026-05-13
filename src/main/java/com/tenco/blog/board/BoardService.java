package com.tenco.blog.board;

import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog.reply.ReplyRepository;
import com.tenco.blog.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * 서비스 레이어
 * 핵심 개념 :
 * 1. 서비스 레이어의 역할:
 * - 비즈니스 로직을 처리하는 계층
 * - Controller와 Response 사이에서 중간 계층 담당
 * - 트랜잭션 관리
 * - 여러 Repository를 조합해서 복잡한 비즈니스 로직 처리
 * <p>
 * 2. 계층 구조 (3Tier 아키텍처)
 * Controller -> Service -> Repository-> DB
 * <p>
 * 3. @Service 어노테이션 사용
 * - Spring이 이 어노테이션을 확인해서 Bean(빈) 등록 한다.
 */


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final ReplyRepository replyRepository;

    /**
     * 게시글 목록 조회
     * OSIV false 환경 대응 - 응답 DTO 설계
     */
    public BoardResponse.PageDTO 게시글목록(Integer page, Integer size,String keyword) {
        // 화면 기준에서 넘어 오는 값(사용자에게 보여지는 기준) 0이아니라 1부터 시작
        // Spring Data JPA 기준으로 offset은 0번부터 시작
        // 사용자가 일부 음수값을 넣더라도 기본 값 0으로 셋팅될 수 있도록 방어적 코드 작성
        int pageIndex = Math.max(0, page - 1);
        // 사용자가 임의로 많은 값을 던지는 것을 방지
        int validSize = Math.max(1,Math.min(50,size));
        Sort sort = Sort.by(Sort.Direction.DESC,"id");
        /**
         * Pageable
         * 어떤 페이지(pageIndex), 몇 개씩(validSize), 어떤 정렬(sort)을 가져올지
         * 한 묶음으로 표현하는 Spring Data 표준 페이징 인터페이스
         * Repository에 Paseable 객체를 넘기면 자동으로 Limit, Offset을 만들어 준다
         */
        Pageable pageable = PageRequest.of(pageIndex,validSize,sort);

        /**
         * Page<T>
         * 조회된 데이터 한 페이지와 페이징 관련된 메타 데이터를 한꺼번에 담아주는 객체
         * - getContent(): 현재 페이지의 데이터 목록
         * - getNumbers(): 현재 페이지 번호(0번부터 시작)
         * - getTotalElements(): 전체 항목 수
         * - getTotalPage(): 전체 페이지 수
         * - isFirst()/ islast(): 첫 페이지 마지막페이지 여부(Boolean)
         */

        Page<Board> boardPage;
        if(keyword == null || keyword.isBlank()) {
            boardPage = boardRepository.findAllWithUserOrderByCreatedAtDesc(pageable);
        } else {
            boardPage = boardRepository.findByTitleContainingOrContentContaining(keyword.trim(),pageable);
        }



        // DTO로 변환해서 컨트롤러로 내려줌(OSIV 대응)
        return new BoardResponse.PageDTO(boardPage);
    }
//    public List<BoardResponse.ListDTO> 게시글목록() {
//        log.info("게시글 목록 조회 서비스");
//        List<Board> boardList = boardRepository.findAllJoinUser();
//        boardList.get(0).getUser(); // LAZY 로딩 --> 한 번더 쿼리 던짐
//        log.info("게시글 목록 조회 완료 - 총 : {}", boardList.size());
//        return boardList.stream()
//                .map(BoardResponse.ListDTO::new) // map 닫기
//                .collect(Collectors.toList());
//    }

    /**
     * 게시글 상세 조회
     *
     * @param id (Board PK)
     * @return DetailDTO 처리 (OSIV 대응)
     */
    public BoardResponse.DetailDTO 게시글상세조회(Integer id) {
        log.info("게시글 상세 조회 서비스");
        // N + 1 문제를 해결하기 위해 한번에 Board, User 가지고 옴
        Board boardEntity = boardRepository.findByIdJoinUser(id).orElseThrow(() -> {
            log.warn("게시글 조회 실패 - ID: {}", id);
            return new Exception404("해당하는 게시글을 찾을 수 없습니다");
        });
        log.info("게시글 조회 완료 - 제목: {}, 작성자: {}",
                boardEntity.getTitle(), boardEntity.getUser().getUsername());

        return new BoardResponse.DetailDTO(boardEntity);
    }


    /**
     * 게시글 작성
     *
     * @param saveDTO
     * @param sessionUser (세션에서 가져온 사용자 정보)
     */
    @Transactional
    public void 게시글작성(BoardRequest.SaveDTO saveDTO, User sessionUser) {
        log.info("게시글 저장 서비스 시작 - 제목 : {}, 작성자 : {}",
                saveDTO.getTitle(), sessionUser.getUsername());
        Board board = saveDTO.toEntity(sessionUser);
        Board savedBoardEntity = boardRepository.save(board);
        log.info("게시글 저장 완료 - ID : {}, 제목 : {}",
                savedBoardEntity.getId(), savedBoardEntity.getTitle());
    }

    /**
     * 게시글 상세 화면 요청(인가 처리)
     *
     * @param id          (Board PK)
     * @param sessionUser (로그인한 사용자 정보)
     * @return BoardResponse.DetailDTO
     */
    public BoardResponse.DetailDTO 게시글상세화면및인가처리(Integer id, User sessionUser) {
        log.info("게시글 상세 화면 및 인가 확인");
        BoardResponse.DetailDTO detailDTO = 게시글상세조회(id);
        if (!detailDTO.getUserId().equals(sessionUser.getId())) {
            throw new Exception403("권한없음");
        }
        log.info("게시글 수정 조회 완료 - 제목: {}, 작성자: {}",
                detailDTO.getTitle(), detailDTO.getUsername());
        return detailDTO;
    }


    /**
     * 게시글 수정 기능 처리
     *
     * @param id          (Board PK)
     * @param updateDTO
     * @param sessionUser
     * @return
     */
    @Transactional
    public void 게시글수정(Integer id, BoardRequest.UpdateDTO updateDTO, User sessionUser) {
        log.info("게시글 수정 서비스");
        Board boardEntity = boardRepository.findByIdJoinUser(id).orElseThrow(() -> {
            throw new Exception404("해당 게시글을 찾을 수 없습니다");
        });
        // 영속화 되어 있었던 객체의 title, content 의 내용이 변경 됨.
        boardEntity.update(updateDTO);

        log.info("게시글 수정 완료 - ID : {}, 새 제목: {}",
                boardEntity.getId(), boardEntity.getTitle());
    }

    /**
     * 게시글 삭제 요청
     *
     * @param id          (Board PK)
     * @param sessionUser
     */
    @Transactional
    public void 게시글삭제(Integer id, User sessionUser) {
        log.info("게시글 삭제 서비스");
        Board boardEntity = boardRepository.findById(id).orElseThrow(
                () -> new Exception404("게시글을 찾을 수 없습니다")
        );
        boardEntity.isOwner(sessionUser.getId());

        // 기존에 작성된 댓글 부터 전체 삭제
        // 게시글 삭제 요청시 해당 게시글에 관련된 댓글 삭제는 어떻게? 만들 수 있음?
        replyRepository.deleteByBoardId(boardEntity.getId());

        boardRepository.deleteById(id);
        log.info("게시글 삭제 완료 - ID : {}", id);
    }
}


