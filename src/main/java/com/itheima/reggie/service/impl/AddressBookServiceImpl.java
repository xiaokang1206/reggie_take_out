package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.mapper.AddressBookMapper;
import com.itheima.reggie.service.AddressBookService;
import org.springframework.stereotype.Service;

@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {

    /**
     * 设置默认地址
     * @param addressBook
     */
    @Override
    public void setDefaultAddress(AddressBook addressBook) {
        //获取需要被设置成默认地址的id
        Long id = addressBook.getId();
        //把当前用户的所有地址设置状态设置为0
        Long currentId = BaseContext.getCurrentId();
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper=new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(AddressBook::getUserId,currentId);
        lambdaUpdateWrapper.set(AddressBook::getIsDefault,0);

        //update addressBook set isDefault=0 where userID=currentID;
        this.update(lambdaUpdateWrapper);


        //把id对应的地址状态设置为1
        addressBook.setIsDefault(1);
        this.updateById(addressBook);

    }

}
