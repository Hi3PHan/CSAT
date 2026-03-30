package group2.CSAT.BMTT.repository;

import group2.CSAT.BMTT.model.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);

    @Modifying
    @Transactional
    void deleteByToken(String token);
}
