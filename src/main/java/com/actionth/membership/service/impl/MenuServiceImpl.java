package com.actionth.membership.service.impl;

import com.actionth.membership.model.Menu;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.MenuDto;
import com.actionth.membership.repository.MenuRepository;
import com.actionth.membership.service.MenuService;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuServiceImpl implements MenuService {

	private final MenuRepository menuRepository;
	private final ModelMapper modelMapper;

	@Override
	public Page<MenuDto> findAll(PagingData paging, Boolean active) {
		Specification<Menu> activeSpec = (root, query, cb) -> {
			if (active != null) {
				return cb.equal(root.get("active"), active);
			} else {
				return null;
			}
		};

		Specification<Menu> searchSpec = (root, query, cb) -> {
			if (paging != null && paging.getSearchField() != null && paging.getSearchText() != null) {
				String pattern = "%" + paging.getSearchText().toLowerCase() + "%";
				return cb.like(cb.lower(root.get(paging.getSearchField())), pattern);
			}
			return null;
		};

		Specification<Menu> combinedSpec = Specification.where(activeSpec).and(searchSpec);

		if (paging == null) {
			List<MenuDto> dtos = menuRepository.findAll(combinedSpec)
					.stream()
					.map(this::mapMenuToDto)
					.toList();
			return new PageImpl<>(dtos);
		} else {
			Pageable pageable = PageRequest.of(
					paging.getPage(),
					paging.getSize(),
					paging.getSortField() != null
							? Sort.by(Sort.Direction.fromString(paging.getSortDirection()), paging.getSortField())
							: Sort.unsorted());

			return menuRepository.findAll(combinedSpec, pageable)
					.map(this::mapMenuToDto);
		}
	}

	@Override
	public MenuDto getMenuByUuid(String uuid) {
		Menu menu = menuRepository.findByUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Menu not found with uuid: " + uuid));
		return modelMapper.map(menu, MenuDto.class);
	}

	@Override
	public MenuDto createMenu(MenuDto dto) {
		Menu menu = modelMapper.map(dto, Menu.class);
		return modelMapper.map(menuRepository.save(menu), MenuDto.class);
	}

	@Override
	public MenuDto updateMenu(MenuDto dto) {
		Menu menu = menuRepository.findByUuid(dto.getId())
				.orElseThrow(() -> new EntityNotFoundException("Menu not found"));

		menu.setTitle(dto.getTitle());
		menu.setPath(dto.getPath());
		menu.setIcon(dto.getIcon());
		menu.setIsDisplay(dto.getIsDisplay());
		menu.setDisabled(dto.getDisabled());
		menu.setIsNoti(dto.getIsNoti());
		menu.setBadgeKey(dto.getBadgeKey());

		return modelMapper.map(menuRepository.save(menu), MenuDto.class);
	}

	@Override
	public void deleteMenu(String uuid, String mode) {
		Menu menu = menuRepository.findByUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Menu not found"));

		if ("soft".equalsIgnoreCase(mode)) {
			menu.setActive(false);
			menuRepository.save(menu);
		} else {
			menuRepository.delete(menu);
		}
	}

	@Override
	public void reorderMenus(List<MenuDto> menus) {
		for (MenuDto dto : menus) {
			Menu menu = menuRepository.findByUuid(dto.getId())
					.orElseThrow(() -> new EntityNotFoundException("Menu not found"));

			menu.setPosition(dto.getPosition());
			menuRepository.save(menu);
		}
	}

	private MenuDto mapMenuToDto(Menu menu) {
		MenuDto menuDto = modelMapper.map(menu, MenuDto.class);
		menuDto.setId(menu.getUuid());
		return menuDto;
	}
}
