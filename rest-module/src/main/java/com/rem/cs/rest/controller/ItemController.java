package com.rem.cs.rest.controller;

import com.rem.cs.commons.NumberUtils;
import com.rem.cs.data.jpa.specification.EntitySpecificationBuilder;
import com.rem.cs.data.jpa.entity.Category;
import com.rem.cs.data.jpa.entity.Item;
import com.rem.cs.data.jpa.repository.ItemRepository;
import com.rem.cs.exception.EntityException;
import com.rem.cs.rest.dto.CategoryDto;
import com.rem.cs.rest.dto.ItemDto;
import com.rem.mappyfy.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController("itemRestController")
@RequestMapping("/api/items")
public class ItemController {

    private ItemRepository itemRepo;

    @Autowired
    public ItemController(ItemRepository itemRepo) {
        this.itemRepo = itemRepo;
    }

    @GetMapping(produces = {"application/json", "application/hal+json"})
    public ResponseEntity<?> findAll(@RequestParam(name = "search", required = false) String search,
                                     @RequestParam(name = "page", required = false) Integer page,
                                     @RequestParam(name = "size", required = false) Integer size,
                                     @RequestParam(name = "sort", required = false) String sort) {
        final EntitySpecificationBuilder<Item> builder = new EntitySpecificationBuilder<>();
        final List<ItemDto> items = new ArrayList<>();
        final Mapper mapper = new Mapper();

        final int tempPage = NumberUtils.zeroBased(page);
        final int tempSize = NumberUtils.thisOr(size, 20).intValue();
        final String tempSort = !StringUtils.isEmpty(sort) ? sort : "name";

        final Pageable pageable = PageRequest.of(tempPage, tempSize, Sort.by(tempSort));

        final PagedResources<ItemDto> resources;
        final Page<Item> pageItem;

        if (!StringUtils.isEmpty(search)) {
            final Pattern pattern = Pattern.compile("(\\w+?)(:|<|>)(\\w+?),");
            final Matcher matcher = pattern.matcher(search + ",");

            while (matcher.find()) {
                builder.with(matcher.group(1), matcher.group(2), matcher.group(3));
            }
            if (builder.getParamSize() == 0) {
                builder.with("name", ":", search);
            }
        }
        pageItem = itemRepo.findAll(builder.build(), pageable);
        pageItem.forEach(item -> {
            final ItemDto dto = mapper.from(item).toInstanceOf(ItemDto.class);

            dto.add(linkTo(methodOn(getClass()).findById(dto.getUid())).withSelfRel());
            dto.add(linkTo(methodOn(getClass()).findCategoriesById(dto.getUid(), null, null, null))
                    .withRel("categories"));
            items.add(dto);
        });
        resources = new PagedResources<>(items, new PagedResources.PageMetadata(tempSize, (tempPage + 1), pageItem
                .getTotalElements(), pageItem.getTotalPages()));

        resources.add(linkTo(methodOn(getClass()).findAll(search, page, size, sort)).withSelfRel());
        if (pageItem.getTotalPages() > 1) {
            resources.add(linkTo(methodOn(getClass()).findAll(search, 1, size, sort)).withRel("first"));
            resources.add(linkTo(methodOn(getClass()).findAll(search, pageItem.getTotalPages(), size, sort))
                    .withRel("last"));
        }
        if ((tempPage + 1) > 1 && (tempPage + 1) <= pageItem.getTotalPages() && !pageItem.isFirst()) {
            resources.add(linkTo(methodOn(getClass()).findAll(search, (tempPage + 1) - 1, size, sort))
                    .withRel("prev"));
        }
        if (!pageItem.isLast()) {
            resources.add(linkTo(methodOn(getClass()).findAll(search, (tempPage + 1) + 1, size, sort))
                    .withRel("next"));
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @GetMapping(value = {"/{itemId}"}, produces = {"application/json", "application/hal+json"})
    public ResponseEntity<?> findById(@PathVariable("itemId") String itemId) {
        final Mapper mapper = new Mapper();
        final Optional<Item> item = itemRepo.findById(itemId);

        try {
            final ItemDto dto = mapper.from(item.orElseThrow(() -> new EntityException("No item found")))
                    .toInstanceOf(ItemDto.class);
            final Resource<ItemDto> resource = new Resource<>(dto);

            resource.add(linkTo(methodOn(getClass()).findById(itemId)).withSelfRel());
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (EntityException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = {"/search/findByCategoryId"}, produces = {"application/json", "application/hal+json"})
    public ResponseEntity<?> findByCategoryId(@RequestParam(name = "categoryId") List<String> categoryIds,
                                              @RequestParam(name = "page", required = false) Integer page,
                                              @RequestParam(name = "size", required = false) Integer size,
                                              @RequestParam(name = "sort", required = false) String sort) {
        final List<ItemDto> items = new ArrayList<>();
        final Mapper mapper = new Mapper();

        final int tempPage = NumberUtils.zeroBased(page);
        final int tempSize = NumberUtils.thisOr(size, 20).intValue();
        final String tempSort = !StringUtils.isEmpty(sort) ? sort : "name";

        final Pageable pageable = PageRequest.of(tempPage, tempSize, Sort.by(tempSort));
        final Page<Item> pageItem = itemRepo.findByCategoryId(categoryIds, pageable);

        final PagedResources<ItemDto> resources;

        pageItem.forEach(item -> {
            final ItemDto dto = mapper.from(item).toInstanceOf(ItemDto.class);

            dto.add(linkTo(methodOn(getClass()).findById(dto.getUid())).withSelfRel());
            items.add(dto);
        });
        resources = new PagedResources<>(items, new PagedResources.PageMetadata(tempSize, (tempPage + 1), pageItem
                .getTotalElements(), pageItem.getTotalPages()));

        resources.add(linkTo(methodOn(getClass()).findByCategoryId(categoryIds, page, size, sort)).withSelfRel());
        if (pageItem.getTotalPages() > 1) {
            resources.add(linkTo(methodOn(getClass()).findByCategoryId(categoryIds, 1, size, sort))
                    .withRel("first"));
            resources.add(linkTo(methodOn(getClass()).findByCategoryId(categoryIds, pageItem
                    .getTotalPages(), size, sort)).withRel("last"));
        }
        if ((tempPage + 1) > 1 && (tempPage + 1) <= pageItem.getTotalPages() && !pageItem.isFirst()) {
            resources.add(linkTo(methodOn(getClass()).findByCategoryId(categoryIds, (tempPage + 1) - 1, size, sort))
                    .withRel("prev"));
        }
        if (!pageItem.isLast()) {
            resources.add(linkTo(methodOn(getClass()).findByCategoryId(categoryIds, (tempPage + 1) + 1, size, sort))
                    .withRel("next"));
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @GetMapping(value = {"/{itemId}/categories"}, produces = {"application/json", "application/hal+json"})
    public ResponseEntity<?> findCategoriesById(@PathVariable("itemId") String itemId,
                                                @RequestParam(name = "page", required = false) Integer page,
                                                @RequestParam(name = "size", required = false) Integer size,
                                                @RequestParam(name = "sort", required = false) String sort) {
        final List<CategoryDto> categories = new ArrayList<>();
        final Mapper mapper = new Mapper();

        final int tempPage = NumberUtils.zeroBased(page);
        final int tempSize = NumberUtils.thisOr(size, 20).intValue();
        final String tempSort = !StringUtils.isEmpty(sort) ? sort : "name";

        final Pageable pageable = PageRequest.of(tempPage, tempSize, Sort.by(tempSort));
        final Page<Category> pageCategory = itemRepo.findCategoriesById(itemId, pageable);

        final PagedResources<CategoryDto> resources;

        pageCategory.forEach(category -> {
            final CategoryDto dto = mapper.from(category).toInstanceOf(CategoryDto.class);

            dto.add(linkTo(methodOn(CategoryController.class).findById(dto.getUid())).withSelfRel());
            categories.add(dto);
        });
        resources = new PagedResources<>(categories, new PagedResources.PageMetadata(tempSize, (tempPage + 1),
                pageCategory.getTotalElements(), pageCategory.getTotalPages()));

        resources.add(linkTo(methodOn(getClass()).findCategoriesById(itemId, page, size, sort)).withSelfRel());
        if (pageCategory.getTotalPages() > 1) {
            resources.add(linkTo(methodOn(getClass()).findCategoriesById(itemId, 1, size, sort))
                    .withRel("first"));
            resources.add(linkTo(methodOn(getClass()).findCategoriesById(itemId, pageCategory
                    .getTotalPages(), size, sort)).withRel("last"));
        }
        if ((tempPage + 1) > 1 && (tempPage + 1) <= pageCategory.getTotalPages() && !pageCategory.isFirst()) {
            resources.add(linkTo(methodOn(getClass()).findCategoriesById(itemId, (tempPage + 1) - 1, size, sort))
                    .withRel("prev"));
        }
        if (!pageCategory.isLast()) {
            resources.add(linkTo(methodOn(getClass()).findCategoriesById(itemId, (tempPage + 1) + 1, size, sort))
                    .withRel("next"));
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }
}
