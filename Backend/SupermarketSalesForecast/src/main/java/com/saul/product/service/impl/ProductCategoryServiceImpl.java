package com.saul.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.product.dto.CategoryAddDTO;
import com.saul.product.dto.CategoryUpdateDTO;
import com.saul.product.entity.ProductCategory;
import com.saul.product.mapper.ProductCategoryMapper;
import com.saul.product.service.IProductCategoryService;
import com.saul.product.vo.CategoryTreeVO;
import com.saul.product.mapper.ProductMapper;
import com.saul.product.entity.Product;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品分类业务实现。
 */
@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
        implements IProductCategoryService {

    @Resource
    private ProductMapper productMapper;

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        // 1) 一次性查询全部分类（禁止在循环里查库）
        List<ProductCategory> list = this.list(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder)
                .orderByAsc(ProductCategory::getId));

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 将扁平的实体列表转换为 VO（VO 才能包含 children 字段，且不直接暴露实体）
        List<CategoryTreeVO> voList = list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // 3) 按 parentId 分组：key=父ID，value=该父ID下的子节点列表
        //    这一步是高效组装树形结构的关键：O(n) 建索引，避免递归时反复扫全表。
        Map<Long, List<CategoryTreeVO>> childrenMap = voList.stream()
                .collect(Collectors.groupingBy(vo -> vo.getParentId() == null ? 0L : vo.getParentId()));

        // 4) 找到顶级节点（parentId == 0），并递归填充 children
        List<CategoryTreeVO> roots = childrenMap.getOrDefault(0L, Collections.emptyList());
        roots.sort(categoryComparator());
        roots.forEach(root -> fillChildren(root, childrenMap));

        return roots;
    }

    /**
     * 递归为当前节点填充 children，并对每一层按 sortOrder 升序排序。
     */
    private void fillChildren(CategoryTreeVO node, Map<Long, List<CategoryTreeVO>> childrenMap) {
        if (node == null || node.getId() == null) return;

        List<CategoryTreeVO> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        if (children.isEmpty()) {
            node.setChildren(Collections.emptyList());
            return;
        }

        children.sort(categoryComparator());
        node.setChildren(children);

        for (CategoryTreeVO child : children) {
            fillChildren(child, childrenMap);
        }
    }

    /**
     * 排序规则：sortOrder 升序；若 sortOrder 为空则视为较大；再按 id 升序兜底，保证稳定输出。
     */
    private Comparator<CategoryTreeVO> categoryComparator() {
        return Comparator
                .comparing(CategoryTreeVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryTreeVO::getId, Comparator.nullsLast(Long::compareTo));
    }

    private CategoryTreeVO toVO(ProductCategory c) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setParentId(c.getParentId() == null ? 0L : c.getParentId());
        vo.setLevel(c.getLevel());
        vo.setSortOrder(c.getSortOrder());
        vo.setStatus(c.getStatus());
        return vo;
    }

    @Override
    public void addCategory(CategoryAddDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        // 1) 处理默认值（防御性编程：避免前端漏传）
        String name = dto.getName() == null ? null : dto.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("分类名称不能为空");
        }

        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();
        Integer sortOrder = dto.getSortOrder() == null ? 0 : dto.getSortOrder();

        // 2) 同级防重：同一个 parentId 下不允许存在同名分类
        Long cnt = this.lambdaQuery()
                .eq(ProductCategory::getParentId, parentId)
                .eq(ProductCategory::getName, name)
                .count();
        if (cnt != null && cnt > 0) {
            throw new RuntimeException("同一级别下已存在该分类名称");
        }

        // 3) 层级处理：如果未传 level，则根据 parentId 动态计算，且不超过3层
        Integer level = dto.getLevel();
        if (level == null) {
            if (parentId == 0L) {
                level = 1;
            } else {
                ProductCategory parent = this.getById(parentId);
                if (parent == null) {
                    throw new RuntimeException("父级分类不存在");
                }
                Integer parentLevel = parent.getLevel() == null ? 1 : parent.getLevel();
                level = parentLevel + 1;
            }
        }
        if (level > 3) {
            throw new RuntimeException("分类层级最多3层，无法继续添加子分类");
        }

        // 4) DTO -> Entity 并保存入库
        ProductCategory entity = new ProductCategory();
        entity.setName(name);
        entity.setParentId(parentId);
        entity.setLevel(level);
        entity.setSortOrder(sortOrder);
        entity.setStatus(status);

        boolean ok = this.save(entity);
        if (!ok) {
            throw new RuntimeException("添加分类失败");
        }
    }

    @Override
    public void updateCategory(CategoryUpdateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        // 1) 基础存在性校验：分类必须存在
        ProductCategory current = this.getById(dto.getId());
        if (current == null) {
            throw new RuntimeException("分类不存在");
        }

        // 2) 循环引用防御：检查新父节点是否是当前节点的子孙节点（含自身）
        if (dto.getParentId() != null && dto.getParentId() != 0L) {
            if (dto.getId().equals(dto.getParentId())) {
                throw new IllegalArgumentException("父级分类不能是自己");
            }
            if (isDescendant(dto.getId(), dto.getParentId())) {
                throw new IllegalArgumentException("不能将分类移动到自身的子孙节点下，会形成循环引用");
            }
        }

        // 计算“目标” parentId / name（用于同级防重）
        Long targetParentId = dto.getParentId() == null ? (current.getParentId() == null ? 0L : current.getParentId()) : dto.getParentId();
        String targetName = dto.getName() == null ? current.getName() : dto.getName().trim();
        if (targetName == null || targetName.isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        boolean parentChanged = dto.getParentId() != null && !targetParentId.equals(current.getParentId() == null ? 0L : current.getParentId());
        boolean nameChanged = dto.getName() != null && !targetName.equals(current.getName());

        // 3) 同级防重校验：如果修改了 name 或 parentId，则检查目标 parentId 下是否存在同名分类（排除自身）
        if (parentChanged || nameChanged) {
            Long dupCnt = this.lambdaQuery()
                    .eq(ProductCategory::getParentId, targetParentId)
                    .eq(ProductCategory::getName, targetName)
                    .ne(ProductCategory::getId, dto.getId())
                    .count();
            if (dupCnt != null && dupCnt > 0) {
                throw new RuntimeException("同一级别下已存在该分类名称");
            }
        }

        // 4) 动态层级计算：如果 parentId 发生变化，则重新计算 level（父级 level + 1；顶级为 1）
        Integer newLevel = null;
        if (parentChanged) {
            if (targetParentId == 0L) {
                newLevel = 1;
            } else {
                ProductCategory parent = this.getById(targetParentId);
                if (parent == null) {
                    throw new RuntimeException("父级分类不存在");
                }
                Integer parentLevel = parent.getLevel() == null ? 1 : parent.getLevel();
                newLevel = parentLevel + 1;
            }
            if (newLevel != null && newLevel > 3) {
                throw new RuntimeException("分类层级最多3层，无法移动到该位置");
            }
        }

        // 5) 按需更新：仅更新前端传了非空值的字段
        ProductCategory update = new ProductCategory();
        update.setId(dto.getId());
        if (dto.getName() != null) {
            update.setName(targetName);
        }
        if (dto.getParentId() != null) {
            update.setParentId(targetParentId);
            update.setLevel(newLevel);
        }
        if (dto.getSortOrder() != null) {
            update.setSortOrder(dto.getSortOrder());
        }
        if (dto.getStatus() != null) {
            // 5a) 禁用前置校验：该分类及所有子分类下不能存在在售商品
            if (dto.getStatus() == 0 && (current.getStatus() == null || current.getStatus() == 1)) {
                long activeCount = countActiveProductsUnder(dto.getId());
                if (activeCount > 0) {
                    throw new RuntimeException(
                        "该分类下还有 " + activeCount + " 件在售商品，请先将商品全部下架后再禁用分类");
                }
            }
            update.setStatus(dto.getStatus());
        }

        boolean ok = this.updateById(update);
        if (!ok) {
            throw new RuntimeException("修改分类失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分类ID不能为空");
        }

        // 1) 子节点关联校验：如果当前分类下存在子分类，则不允许删除（避免树形结构断裂）
        Long childrenCount = this.lambdaQuery()
                .eq(ProductCategory::getParentId, id)
                .count();
        if (childrenCount != null && childrenCount > 0) {
            throw new RuntimeException("该分类下存在子分类，无法直接删除");
        }

        // 2) 商品关联校验：如果该分类下有关联商品，则不允许删除
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new RuntimeException("该分类下已关联商品，无法直接删除！建议先清理商品或修改分类。");
        }

        // 3) 执行删除：仅当通过所有校验后，才允许物理删除
        boolean ok = this.removeById(id);
        if (!ok) {
            throw new RuntimeException("删除分类失败");
        }
    }

    /**
     * 统计指定分类及其所有子孙分类下的在售商品数量。
     * 用于禁用分类时的前置校验：必须先下架所有商品才能禁用分类。
     */
    private long countActiveProductsUnder(Long categoryId) {
        // 收集该分类及所有子孙分类的ID（BFS）
        Set<Long> allCategoryIds = new HashSet<>();
        Queue<Long> queue = new java.util.LinkedList<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long cid = queue.poll();
            allCategoryIds.add(cid);
            List<ProductCategory> children = this.lambdaQuery()
                    .eq(ProductCategory::getParentId, cid)
                    .list();
            for (ProductCategory child : children) {
                queue.add(child.getId());
            }
        }
        // 查询这些分类下的在售商品数
        return productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .in(Product::getCategoryId, allCategoryIds)
                .eq(Product::getStatus, 1));
    }

    /**
     * 判断 targetId 是否是 ancestorId 的子孙节点（含自身）。
     * 用于修改父节点时防止形成循环引用：A→B→C，若把A的父节点改为C，则形成环。
     *
     * @param ancestorId 祖先节点ID（即被修改的分类自身）
     * @param targetId   目标父节点ID（即新的父节点）
     * @return true 表示 targetId 是 ancestorId 的子孙，移动会形成循环
     */
    private boolean isDescendant(Long ancestorId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && current != 0L) {
            if (current.equals(ancestorId)) {
                return true; // targetId 在 ancestorId 的子树中，移动会形成环
            }
            if (visited.contains(current)) {
                break; // 已存在的循环，提前终止防止死循环
            }
            visited.add(current);
            ProductCategory node = this.getById(current);
            current = (node == null) ? null : node.getParentId();
        }
        return false;
    }
}

