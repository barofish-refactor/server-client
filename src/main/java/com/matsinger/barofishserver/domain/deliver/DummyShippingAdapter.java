package com.matsinger.barofishserver.domain.deliver;

import com.matsinger.barofishserver.domain.deliver.domain.Deliver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("local")
public class DummyShippingAdapter implements ShippingApiAdapter {
    @Override
    public List<Deliver.Company> getRecommendDeliverCompanyList(String invoice) {
        return null;
    }

    @Override
    public String getAccessKey() {
        return null;
    }

    @Override
    public Deliver.TrackingInfo getTrackingInfo(String code, String invoice) {
        return null;
    }
}
