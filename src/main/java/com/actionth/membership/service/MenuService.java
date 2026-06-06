package com.actionth.membership.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.MenuDto;

public interface MenuService {

    Page<MenuDto> findAll(PagingData paging, Boolean active);

    MenuDto getMenuByUuid(String uuid);

    MenuDto createMenu(MenuDto dto);

    MenuDto updateMenu(MenuDto dto);

    void deleteMenu(String uuid, String mode);

    void reorderMenus(List<MenuDto> menus);

}
