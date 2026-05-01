package com.example.demo.dto.admin.school;

import com.example.demo.entities.SchoolStatus;
import java.time.LocalDateTime;

public class SchoolDetailResponse {

    private Long id;
    private String subdomain;
    private String smartschoolUrl;
    private String naam;
    private String adres;
    private Double latitude;
    private Double longitude;
    private SchoolStatus status;
    private LocalDateTime aangemaaktOp;
    private long userCount;
    private long klasCount;
    private long bookCount;
    private long activeLoansCount;
    private long wishlistCount;
    private long classReadingListCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getSmartschoolUrl() { return smartschoolUrl; }
    public void setSmartschoolUrl(String smartschoolUrl) { this.smartschoolUrl = smartschoolUrl; }

    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }

    public String getAdres() { return adres; }
    public void setAdres(String adres) { this.adres = adres; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public SchoolStatus getStatus() { return status; }
    public void setStatus(SchoolStatus status) { this.status = status; }

    public LocalDateTime getAangemaaktOp() { return aangemaaktOp; }
    public void setAangemaaktOp(LocalDateTime aangemaaktOp) { this.aangemaaktOp = aangemaaktOp; }

    public long getUserCount() { return userCount; }
    public void setUserCount(long userCount) { this.userCount = userCount; }

    public long getKlasCount() { return klasCount; }
    public void setKlasCount(long klasCount) { this.klasCount = klasCount; }

    public long getBookCount() { return bookCount; }
    public void setBookCount(long bookCount) { this.bookCount = bookCount; }

    public long getActiveLoansCount() { return activeLoansCount; }
    public void setActiveLoansCount(long activeLoansCount) { this.activeLoansCount = activeLoansCount; }

    public long getWishlistCount() { return wishlistCount; }
    public void setWishlistCount(long wishlistCount) { this.wishlistCount = wishlistCount; }

    public long getClassReadingListCount() { return classReadingListCount; }
    public void setClassReadingListCount(long classReadingListCount) { this.classReadingListCount = classReadingListCount; }
}
