package com.helltractor.exchange.quotation;

import com.helltractor.exchange.entity.quatation.*;
import com.helltractor.exchange.support.AbstractDbService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class QuotationDbService extends AbstractDbService {
    
    public void saveBars(SecBarEntity sec, MinBarEntity min, HourBarEntity hour, DayBarEntity day) {
        if (sec != null) {
            this.dataBase.insertIgnore(sec);
        }
        if (min != null) {
            this.dataBase.insertIgnore(min);
        }
        if (hour != null) {
            this.dataBase.insertIgnore(hour);
        }
        if (day != null) {
            this.dataBase.insertIgnore(day);
        }
    }
    
    public void saveTicks(List<TickEntity> ticks) {
        this.dataBase.insertIgnore(ticks);
    }
    
}
