package com.helltractor.exchange.quotation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.helltractor.exchange.model.quatation.DayBarEntity;
import com.helltractor.exchange.model.quatation.HourBarEntity;
import com.helltractor.exchange.model.quatation.MinBarEntity;
import com.helltractor.exchange.model.quatation.SecBarEntity;
import com.helltractor.exchange.model.quatation.TickEntity;
import com.helltractor.exchange.support.AbstractDbService;

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
