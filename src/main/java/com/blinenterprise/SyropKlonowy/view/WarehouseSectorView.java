package com.blinenterprise.SyropKlonowy.view;

import com.blinenterprise.SyropKlonowy.domain.AmountOfProduct;
import com.blinenterprise.SyropKlonowy.domain.WarehouseSector;
import com.blinenterprise.SyropKlonowy.web.View;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class WarehouseSectorView implements View {
    private Long id;
    private String name;
    private Map<Long, AmountOfProduct> amountOfProducts;

    public static WarehouseSectorView from(WarehouseSector warehouseSector) {
        return new WarehouseSectorView(
                warehouseSector.getId(),
                warehouseSector.getName(),
                warehouseSector.getAmountOfProducts());
    }

    public WarehouseSectorView(Long id, String name, Map<Long, AmountOfProduct> amountOfProducts) {
        this.id = id;
        this.name = name;
        this.amountOfProducts = amountOfProducts;
    }

    public static List<WarehouseSectorView> from(List<WarehouseSector> warehouseSectors) {
        ArrayList<WarehouseSectorView> warehouseSectorViewList = new ArrayList<WarehouseSectorView>();
        for (WarehouseSector warehouseSector : warehouseSectors) {
            WarehouseSectorView wsv = from(warehouseSector);
            warehouseSectorViewList.add(wsv);
        }
        return warehouseSectorViewList;
    }
}
