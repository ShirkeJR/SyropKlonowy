package com.blinenterprise.SyropKlonowy.service;

import com.blinenterprise.SyropKlonowy.config.ConfigContainer;
import com.blinenterprise.SyropKlonowy.domain.AmountOfProduct;
import com.blinenterprise.SyropKlonowy.domain.Delivery.ProductWithQuantity;
import com.blinenterprise.SyropKlonowy.domain.Product;
import com.blinenterprise.SyropKlonowy.domain.SaleOrder.SaleOrder;
import com.blinenterprise.SyropKlonowy.domain.SaleOrder.SaleOrderStatus;
import com.blinenterprise.SyropKlonowy.order.OrderClosureExecutor;
import com.blinenterprise.SyropKlonowy.repository.SaleOrderRepository;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SaleOrderService {

    @Autowired
    private SaleOrderRepository saleOrderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private OrderClosureExecutor orderClosureExecutor;

    @Autowired
    private ConfigContainer configContainer;

    @Autowired
    private AmountOfProductService amountOfProductService;

    @Autowired
    private ClientService clientService;

    private Map<Long, SaleOrder> temporarySaleOrders = new HashMap<>();


    public void addProductToOrder(Long clientId, Long productId, Integer quantity) {
        if (clientService.findById(clientId) == null) {
            throw new IllegalArgumentException();
        }
        Product productToAdd = productService.findById(productId).orElseThrow(IllegalArgumentException::new);
        temporarySaleOrders.putIfAbsent(clientId, new SaleOrder(clientId, new Date(), new ArrayList<>(), BigDecimal.valueOf(0), SaleOrderStatus.NEW));
        temporarySaleOrders.get(clientId).addAmountOfProduct(new AmountOfProduct(productToAdd.getId(), quantity));
        temporarySaleOrders.get(clientId).recalculateTotalPrice(productService);
    }

    @Transactional
    public void confirmTempClientOrder(Long clientId) {
        if (!temporarySaleOrders.containsKey(clientId) || temporarySaleOrders.get(clientId).getAmountsOfProducts().isEmpty()) {
            throw new IllegalStateException();
        }
        temporarySaleOrders.get(clientId).getAmountsOfProducts().forEach(amountOfProduct ->
                amountOfProductService.save(amountOfProduct));

        Date closureDate = new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(configContainer.getOrderClosureDelayInDays()));
        orderClosureExecutor.addClosureCommand(temporarySaleOrders.get(clientId).getId(), closureDate);

        saleOrderRepository.save(temporarySaleOrders.get(clientId));
        log.info("Successfully confirmed new order with id:" + temporarySaleOrders.get(clientId).getId());

        temporarySaleOrders.get(clientId).getAmountsOfProducts().forEach(amountOfProduct ->
                warehouseService.removeAmountOfProduct(amountOfProduct, configContainer.getMainWarehouseName()));

        temporarySaleOrders.remove(clientId);
    }

    public SaleOrder create(SaleOrder saleOrder) {
        return saleOrderRepository.save(saleOrder);
    }

    public SaleOrder findById(Long id) {
        Optional<SaleOrder> warehouseById = saleOrderRepository.findById(id);
        return warehouseById.orElse(null);
    }

    public List<SaleOrder> findAll() {
        return Lists.newArrayList(saleOrderRepository.findAll());
    }

    public void deleteById(Long id) {
        saleOrderRepository.deleteById(id);
    }

    public boolean closeById(Long id) {
        Optional<SaleOrder> orderById = saleOrderRepository.findById(id);
        if (orderById.isPresent()) {
            if (orderById.get().closeOrder()) {
                orderById.get().getAmountsOfProducts().forEach(amountOfProduct -> {
                    ProductWithQuantity productWithQuantity = new ProductWithQuantity(
                            productService.findById(amountOfProduct.getProductId()).orElseThrow(IllegalArgumentException::new),
                            amountOfProduct.getQuantity());
                    warehouseService.addProductWithQuantity(productWithQuantity, configContainer.getMainWarehouseName());
                });
                saleOrderRepository.save(orderById.get());
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean payById(Long id) {
        Optional<SaleOrder> orderById = saleOrderRepository.findById(id);
        if (orderById.isPresent()) {
            if (orderById.get().payOrder()) {
                saleOrderRepository.save(orderById.get());
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean sendById(Long id) {
        Optional<SaleOrder> orderById = saleOrderRepository.findById(id);
        if (orderById.isPresent()) {
            if (orderById.get().sendOrder()) {
                saleOrderRepository.save(orderById.get());
                return true;
            } else {
                return false;
            }

        } else {
            throw new IllegalArgumentException();
        }
    }

    public List<SaleOrder> findAllByClientId(Long clientId){
        if(clientService.existById(clientId)) {
            return saleOrderRepository.findAllByClientId(clientId);
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public BigDecimal findMaxPriceInClientOrders(Long clientId){
        if(clientService.existById(clientId)) {
            return saleOrderRepository.findMaxPriceInClientOrders(clientId);
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public BigDecimal findMinPriceInClientOrders(Long clientId) {
        if (clientService.existById(clientId)) {
            return saleOrderRepository.findMinPriceInClientOrders(clientId);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public BigDecimal findMaxPriceOfProductInClientOrders(Long clientId){
        if(clientService.existById(clientId)) {
            return saleOrderRepository.findMaxPriceOfProductInClientOrders(clientId);
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public BigDecimal findAveragePriceOfProductInClientOrders(Long clientId){
        if(clientService.existById(clientId)) {
            return saleOrderRepository.findAveragePriceOfProductInClientOrders(clientId);
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public List<Object[]> findMostCommonlyPurchasedProducts(Long clientId){
        if(clientService.existById(clientId)) {
            List<Object[]> listOfProductIdWithQuantity = saleOrderRepository.findProductIdFromAllOrdersWithSumOfQuantity(clientId);
            listOfProductIdWithQuantity.sort((object1, object2) -> (Long) object1[1]<(Long) object2[1]?1:
                    ((Long)object1[1]==(Long)object2[1]?0:
                            -1));
            return listOfProductIdWithQuantity;
        }
        else{
            throw new IllegalArgumentException();
        }
    }

    public List<Object[]> findFrequentlyBoughtTogether(Long productId){
        if(productService.existsById(productId)) {
            List<Object[]> listOfFrequentlyProduct = saleOrderRepository.findFrequentlyBoughtTogether(productId);
            listOfFrequentlyProduct.sort((object1, object2) -> (Long) object1[1]<(Long) object2[1]?1:
                    ((Long)object1[1]==(Long)object2[1]?0:
                            -1));
            return listOfFrequentlyProduct;
        }
        else{
            throw new IllegalArgumentException();
        }
    }



}
