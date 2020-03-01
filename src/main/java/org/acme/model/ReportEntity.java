package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "report")
public class ReportEntity extends PanacheEntityBase {
    @Id
    public String id;
    public Long startedAt;
    public Long completedAt;
    public Long checked;
    public Long available;
    public Long unavailable;
    public Long amount;
    public Long avgFreq;
    public int cpu;

    public static ReportEntity of(Report report){
        final ReportEntity result = new ReportEntity();
        result.id = report.id();
        result.startedAt = report.startedAt;
        result.completedAt = report.completedAt;
        result.checked = report.checked;
        result.available = report.available;
        result.unavailable = report.unavailable;
        result.amount = report.amount;
        result.avgFreq = report.avgFreq;
        result.cpu = report.cpu;
        return result;
    }

    @Override
    public String toString() {
        return "ReportEntity{" +
                "id='" + id + '\'' +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", checked=" + checked +
                ", available=" + available +
                ", unavailable=" + unavailable +
                ", amount=" + amount +
                ", avgFreq=" + avgFreq +
                ", cpu=" + cpu +
                '}';
    }
}
