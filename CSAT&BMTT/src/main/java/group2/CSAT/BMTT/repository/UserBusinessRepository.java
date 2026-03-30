package group2.CSAT.BMTT.repository;

import group2.CSAT.BMTT.model.entity.UserBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBusinessRepository extends JpaRepository<UserBusiness, Long> {
}
