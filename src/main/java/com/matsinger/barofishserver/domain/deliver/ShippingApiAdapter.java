package com.matsinger.barofishserver.domain.deliver;

import com.matsinger.barofishserver.domain.deliver.domain.Deliver;

import java.util.List;

public interface ShippingApiAdapter {
    List<Deliver.Company> getRecommendDeliverCompanyList(String invoice);
    public String getAccessKey();
    public Deliver.TrackingInfo getTrackingInfo(String code, String invoice);
}
