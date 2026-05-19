package com.example.demo.repositories;

import com.example.demo.entities.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByReturnedAtIsNull();

    List<Loan> findByReturnedAtIsNotNull();

    List<Loan> findByUserSubAndReturnedAtIsNull(String userSub);

    List<Loan> findByUserSubAndReturnedAtIsNotNull(String userSub);

    List<Loan> findByUserSub(String userSub);

    List<Loan> findByCopy_Book_IdAndReturnedAtIsNull(Long bookId);

    List<Loan> findByDueDateAndReturnedAtIsNull(LocalDate dueDate);

    List<Loan> findByDueDateBeforeAndReturnedAtIsNull(LocalDate date);

    void deleteByCopy_Id(Long copyId);

    long countByReturnedAtIsNull();

    long countByCopy_Book_School_Id(Long schoolId);

    long countByCopy_Book_School_IdAndReturnedAtIsNull(Long schoolId);

    @Query("SELECT l.copy.book.id, l.copy.book.titel, l.copy.book.auteur, COUNT(l) FROM Loan l " +
            "WHERE (:schoolId IS NULL OR l.copy.book.school.id = :schoolId) " + 
            "GROUP BY l.copy.book.id, l.copy.book.titel, l.copy.book.auteur " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> findPopularBooksBySchool(@Param("schoolId") Long schoolId);

    @Query("SELECT l.userSub, COUNT(l) FROM Loan l " +
            "WHERE (:schoolId IS NULL OR l.copy.book.school.id = :schoolId) " +
            "GROUP BY l.userSub " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> findTopReadersBySchool(@Param("schoolId") Long schoolId);

    @Query("SELECT u.klas.naam, COUNT(l) FROM Loan l, AppUser u " +
            "WHERE l.userSub = u.sub " +
            "AND (:schoolId IS NULL OR l.copy.book.school.id = :schoolId) " +
            "AND u.klas IS NOT NULL " +
            "GROUP BY u.klas.naam " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> findTopClassesBySchool(@Param("schoolId") Long schoolId);

    @Query("SELECT new map(l.copy.book.id as bookId, COUNT(l) as loanCount) FROM Loan l GROUP BY l.copy.book.id")
    List<Map<String, Object>> getLoanCountsByBook();

    @Query("SELECT au.sub, COUNT(l) FROM Loan l JOIN AppUser au ON l.userSub = au.sub " +
            "WHERE au.klas.id = :klasId " +
            "GROUP BY au.sub " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> findTopReadersByClass(@Param("klasId") Long klasId);

    @Query(value = "SELECT rank_data.`rank`, rank_data.book_count " +
            "FROM ( " +
            "    SELECT au.sub, COUNT(l.id) AS book_count, " +
            "           RANK() OVER (ORDER BY COUNT(l.id) DESC) as `rank` " +
            "    FROM loans l " +
            "    JOIN app_users au ON l.user_sub = au.sub " +
            "    WHERE au.klas_id = :klasId " +
            "    GROUP BY au.sub " +
            ") AS rank_data " +
            "WHERE rank_data.sub = :userSub", nativeQuery = true)
    List<Object[]> findUserRankAndCountInClass(@Param("userSub") String userSub, @Param("klasId") Long klasId);

    @Query(value = "SELECT rank_data.`rank`, rank_data.book_count " +
            "FROM ( " +
            "    SELECT au.sub, COUNT(l.id) AS book_count, " +
            "           RANK() OVER (ORDER BY COUNT(l.id) DESC) as `rank` " +
            "    FROM loans l " +
            "    JOIN app_users au ON l.user_sub = au.sub " +
            "    WHERE au.school_id = :schoolId " +
            "    GROUP BY au.sub " +
            ") AS rank_data " +
            "WHERE rank_data.sub = :userSub", nativeQuery = true)
    List<Object[]> findUserRankAndCountInSchool(@Param("userSub") String userSub, @Param("schoolId") Long schoolId);
}