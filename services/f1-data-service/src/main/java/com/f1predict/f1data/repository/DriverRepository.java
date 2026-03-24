package com.f1predict.f1data.repository;

import com.f1predict.f1data.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, String> {
    List<Driver> findBySeason(int season);
}
