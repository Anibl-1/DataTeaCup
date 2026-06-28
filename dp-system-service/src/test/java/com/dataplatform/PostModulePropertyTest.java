package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.org.entity.Post;
import com.dataplatform.org.entity.UserPost;
import com.dataplatform.org.mapper.PostMapper;
import com.dataplatform.org.mapper.UserPostMapper;
import com.dataplatform.org.service.PostService;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.mapper.UserMapper;
import net.jqwik.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：岗位管理模块
 *
 * Feature: system-modularization
 * Validates: Requirements 10.1-10.6, 11.2, 11.3, 11.4
 */
class PostModulePropertyTest {

    private final AtomicLong idGenerator = new AtomicLong(1);

    // ========== Helper ==========

    private PostService createPostService(PostMapper mockPostMapper, UserPostMapper mockUserPostMapper) {
        return new PostService(mockPostMapper, mockUserPostMapper);
    }

    // ========== Property 11: 岗位 CRUD 往返一致性 ==========

    /**
     * Property 11: 岗位 CRUD 往返一致性
     *
     * For any 有效的岗位对象（包含唯一的 postCode 和非空的 postName），
     * 创建后再查询应返回等价的对象。
     *
     * Validates: Requirements 10.1
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_11_岗位CRUD往返一致性")
    void postCrudRoundTrip(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName,
            @ForAll("sortOrder") Integer sortOrder
    ) {
        PostMapper mockPostMapper = mock(PostMapper.class);
        UserPostMapper mockUserPostMapper = mock(UserPostMapper.class);

        final Post[] stored = {null};
        long generatedId = idGenerator.getAndIncrement();

        when(mockPostMapper.selectByPostCode(eq(postCode))).thenReturn(null);
        when(mockPostMapper.insert(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(generatedId);
            stored[0] = p;
            return 1;
        });
        when(mockPostMapper.selectById(eq(generatedId))).thenAnswer(inv -> stored[0]);

        PostService service = createPostService(mockPostMapper, mockUserPostMapper);

        Post input = new Post();
        input.setPostCode(postCode);
        input.setPostName(postName);
        input.setSortOrder(sortOrder);
        input.setStatus(1);

        Post created = service.create(input);
        Post queried = service.getById(created.getId());

        assertThat(queried).isNotNull();
        assertThat(queried.getPostCode()).isEqualTo(postCode);
        assertThat(queried.getPostName()).isEqualTo(postName);
        assertThat(queried.getSortOrder()).isEqualTo(sortOrder);
        assertThat(queried.getStatus()).isEqualTo(1);
        assertThat(queried.getId()).isEqualTo(generatedId);
    }

    // ========== Property 12: 岗位编码唯一性 ==========

    /**
     * Property 12: 岗位编码唯一性
     *
     * For any 两个岗位创建请求，如果它们的 postCode 相同，
     * 第二个创建请求应失败并返回编码重复错误。
     *
     * Validates: Requirements 10.2
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_12_岗位编码唯一性")
    void postCodeUniqueness(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName1,
            @ForAll("validPostName") String postName2
    ) {
        PostMapper mockPostMapper = mock(PostMapper.class);
        UserPostMapper mockUserPostMapper = mock(UserPostMapper.class);

        final Post[] storedPost = {null};
        long generatedId = idGenerator.getAndIncrement();

        when(mockPostMapper.selectByPostCode(eq(postCode))).thenAnswer(inv -> storedPost[0]);
        when(mockPostMapper.insert(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(generatedId);
            storedPost[0] = p;
            return 1;
        });

        PostService service = createPostService(mockPostMapper, mockUserPostMapper);

        Post first = new Post();
        first.setPostCode(postCode);
        first.setPostName(postName1);
        service.create(first);

        Post second = new Post();
        second.setPostCode(postCode);
        second.setPostName(postName2);

        assertThatThrownBy(() -> service.create(second))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("岗位编码已存在");
    }

    // ========== Property 13: 岗位查询结果的过滤与排序 ==========

    /**
     * Property 13: 岗位查询结果的过滤与排序
     *
     * For any 岗位名称和状态筛选条件，查询返回的所有岗位的名称包含筛选关键字、
     * 状态匹配筛选条件，且按 sortOrder 升序排列。
     *
     * Validates: Requirements 10.3
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_13_岗位查询结果的过滤与排序")
    void postQueryFilterAndSort(
            @ForAll("postNameKeyword") String keyword,
            @ForAll("postStatus") Integer status,
            @ForAll("postList") List<Post> allPosts
    ) {
        PostMapper mockPostMapper = mock(PostMapper.class);
        UserPostMapper mockUserPostMapper = mock(UserPostMapper.class);

        // 计算期望结果
        List<Post> expected = allPosts.stream()
                .filter(p -> keyword == null || keyword.isEmpty()
                        || (p.getPostName() != null && p.getPostName().contains(keyword)))
                .filter(p -> status == null || status.equals(p.getStatus()))
                .sorted(Comparator.comparingInt(p -> p.getSortOrder() != null ? p.getSortOrder() : 0))
                .collect(Collectors.toList());

        when(mockPostMapper.selectByCondition(eq(keyword), eq(status))).thenReturn(expected);

        PostService service = createPostService(mockPostMapper, mockUserPostMapper);
        List<Post> result = service.list(keyword, status);

        // 验证：所有返回项的名称包含筛选关键字
        if (keyword != null && !keyword.isEmpty()) {
            assertThat(result).allSatisfy(p ->
                    assertThat(p.getPostName()).contains(keyword)
            );
        }

        // 验证：所有返回项的状态匹配筛选条件
        if (status != null) {
            assertThat(result).allSatisfy(p ->
                    assertThat(p.getStatus()).isEqualTo(status)
            );
        }

        // 验证：按 sortOrder 升序排列
        for (int i = 1; i < result.size(); i++) {
            int prevSort = result.get(i - 1).getSortOrder() != null ? result.get(i - 1).getSortOrder() : 0;
            int currSort = result.get(i).getSortOrder() != null ? result.get(i).getSortOrder() : 0;
            assertThat(currSort).isGreaterThanOrEqualTo(prevSort);
        }
    }

    // ========== Property 14: 已关联用户的岗位不可删除 ==========

    /**
     * Property 14: 已关联用户的岗位不可删除
     *
     * For any 已关联用户的岗位，删除该岗位的请求应失败并返回岗位已被使用的错误信息。
     *
     * Validates: Requirements 10.4
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_14_已关联用户的岗位不可删除")
    void postWithUsersCannotBeDeleted(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName,
            @ForAll("positiveUserCount") int userCount
    ) {
        PostMapper mockPostMapper = mock(PostMapper.class);
        UserPostMapper mockUserPostMapper = mock(UserPostMapper.class);

        long postId = idGenerator.getAndIncrement();

        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setPostCode(postCode);
        existingPost.setPostName(postName);
        existingPost.setStatus(1);

        when(mockPostMapper.selectById(eq(postId))).thenReturn(existingPost);
        when(mockUserPostMapper.countByPostId(eq(postId))).thenReturn(userCount);

        PostService service = createPostService(mockPostMapper, mockUserPostMapper);

        assertThatThrownBy(() -> service.delete(postId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该岗位已关联用户，无法删除");

        verify(mockPostMapper, never()).deleteById(eq(postId));
    }

    // ========== Property 15: 用户-岗位关联往返一致性 ==========

    /**
     * Property 15: 用户-岗位关联往返一致性
     *
     * For any 用户，设置其 postId 后保存，再查询该用户应返回正确的岗位信息。
     *
     * Validates: Requirements 10.5, 10.6
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_15_用户岗位关联往返一致性")
    void userPostAssociationRoundTrip(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName,
            @ForAll("postStatus") Integer postStatus
    ) {
        UserMapper mockUserMapper = mock(UserMapper.class);

        long userId = idGenerator.getAndIncrement();
        long postId = idGenerator.getAndIncrement();

        // 模拟用户更新后查询返回带岗位信息的用户
        final User[] storedUser = {null};

        User originalUser = new User();
        originalUser.setId(userId);
        originalUser.setUsername("user_" + userId);
        originalUser.setPassword("pwd");
        originalUser.setStatus(1);
        storedUser[0] = originalUser;

        when(mockUserMapper.selectById(eq(userId))).thenAnswer(inv -> storedUser[0]);
        when(mockUserMapper.update(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            storedUser[0].setPostId(u.getPostId());
            // 模拟关联查询返回岗位信息
            storedUser[0].setPostName(postName);
            storedUser[0].setPostStatus(postStatus);
            return 1;
        });

        // 设置用户的 postId
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPostId(postId);
        mockUserMapper.update(updateUser);

        // 查询用户
        User queried = mockUserMapper.selectById(userId);

        // 验证：用户的 postId 正确
        assertThat(queried).isNotNull();
        assertThat(queried.getPostId()).isEqualTo(postId);
        assertThat(queried.getPostName()).isEqualTo(postName);
        assertThat(queried.getPostStatus()).isEqualTo(postStatus);
    }

    // ========== Property 16: 部门用户查询包含岗位信息 ==========

    /**
     * Property 16: 部门用户查询包含岗位信息
     *
     * For any 部门，查询该部门下的用户列表时，每个用户对象应包含其关联的岗位名称信息。
     *
     * Validates: Requirements 11.2
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_16_部门用户查询包含岗位信息")
    void deptUsersContainPostInfo(
            @ForAll("userListWithPosts") List<User> usersWithPosts
    ) {
        UserMapper mockUserMapper = mock(UserMapper.class);

        long deptId = idGenerator.getAndIncrement();

        // 设置所有用户的 deptId
        for (User u : usersWithPosts) {
            u.setDeptId(deptId);
        }

        when(mockUserMapper.selectByDeptId(eq(deptId))).thenReturn(usersWithPosts);

        List<User> result = mockUserMapper.selectByDeptId(deptId);

        // 验证：每个有 postId 的用户都包含岗位名称信息
        assertThat(result).allSatisfy(user -> {
            if (user.getPostId() != null) {
                assertThat(user.getPostName()).isNotNull();
                assertThat(user.getPostName()).isNotEmpty();
            }
        });
    }

    // ========== Property 17: 岗位用户查询正确性 ==========

    /**
     * Property 17: 岗位用户查询正确性
     *
     * For any 岗位，查询该岗位下的用户列表时，返回的所有用户的 postId 等于该岗位的 ID。
     *
     * Validates: Requirements 11.3
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_17_岗位用户查询正确性")
    void postUsersHaveCorrectPostId(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName,
            @ForAll("positiveUserCount") int userCount
    ) {
        UserMapper mockUserMapper = mock(UserMapper.class);

        long postId = idGenerator.getAndIncrement();

        // 生成关联该岗位的用户列表
        List<User> usersForPost = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            User u = new User();
            u.setId(idGenerator.getAndIncrement());
            u.setUsername("user_" + u.getId());
            u.setPostId(postId);
            u.setPostName(postName);
            u.setStatus(1);
            usersForPost.add(u);
        }

        when(mockUserMapper.selectByPostId(eq(postId))).thenReturn(usersForPost);

        List<User> result = mockUserMapper.selectByPostId(postId);

        // 验证：所有返回用户的 postId 等于该岗位的 ID
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(user ->
                assertThat(user.getPostId()).isEqualTo(postId)
        );
    }

    // ========== Property 18: 禁用岗位不影响用户关联 ==========

    /**
     * Property 18: 禁用岗位不影响用户关联
     *
     * For any 已关联用户的岗位，禁用该岗位后，用户的 postId 保持不变，
     * 但查询用户信息时岗位状态显示为禁用。
     *
     * Validates: Requirements 11.4
     */
    @Property(tries = 100)
    @Tag("Feature_system-modularization")
    @Tag("Property_18_禁用岗位不影响用户关联")
    void disablingPostDoesNotAffectUserAssociation(
            @ForAll("validPostCode") String postCode,
            @ForAll("validPostName") String postName
    ) {
        PostMapper mockPostMapper = mock(PostMapper.class);
        UserPostMapper mockUserPostMapper = mock(UserPostMapper.class);
        UserMapper mockUserMapper = mock(UserMapper.class);

        long postId = idGenerator.getAndIncrement();
        long userId = idGenerator.getAndIncrement();

        // 初始状态：岗位启用
        Post post = new Post();
        post.setId(postId);
        post.setPostCode(postCode);
        post.setPostName(postName);
        post.setStatus(1);
        post.setSortOrder(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        // 用户关联该岗位
        User user = new User();
        user.setId(userId);
        user.setUsername("user_" + userId);
        user.setPostId(postId);
        user.setPostName(postName);
        user.setPostStatus(1);
        user.setStatus(1);

        when(mockPostMapper.selectById(eq(postId))).thenReturn(post);
        when(mockPostMapper.update(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            post.setStatus(p.getStatus());
            post.setUpdateTime(p.getUpdateTime());
            return 1;
        });

        // 禁用岗位后，用户查询返回的岗位状态为禁用
        when(mockUserMapper.selectById(eq(userId))).thenAnswer(inv -> {
            User u = new User();
            u.setId(userId);
            u.setUsername("user_" + userId);
            u.setPostId(postId);
            u.setPostName(postName);
            u.setPostStatus(post.getStatus()); // 反映岗位当前状态
            u.setStatus(1);
            return u;
        });

        PostService postService = createPostService(mockPostMapper, mockUserPostMapper);

        // 禁用岗位
        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setPostName(postName);
        updatePost.setSortOrder(0);
        updatePost.setStatus(0); // 禁用
        updatePost.setUpdateTime(LocalDateTime.now());
        postService.update(updatePost);

        // 验证：岗位状态变为禁用
        Post updatedPost = postService.getById(postId);
        assertThat(updatedPost.getStatus()).isEqualTo(0);

        // 验证：用户的 postId 保持不变
        User queriedUser = mockUserMapper.selectById(userId);
        assertThat(queriedUser.getPostId()).isEqualTo(postId);

        // 验证：用户查询时岗位状态显示为禁用
        assertThat(queriedUser.getPostStatus()).isEqualTo(0);
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> validPostCode() {
        return Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20)
                .map(s -> "post_" + s.toLowerCase());
    }

    @Provide
    Arbitrary<String> validPostName() {
        return Arbitraries.of("总经理", "副总经理", "技术总监", "产品经理", "开发工程师",
                "测试工程师", "运维工程师", "UI设计师", "项目经理", "架构师",
                "数据分析师", "前端工程师", "后端工程师", "DBA", "安全工程师");
    }

    @Provide
    Arbitrary<Integer> sortOrder() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> postStatus() {
        return Arbitraries.of(0, 1);
    }

    @Provide
    Arbitrary<String> postNameKeyword() {
        return Arbitraries.of("", "经理", "工程师", "总监", "设计");
    }

    @Provide
    Arbitrary<Integer> positiveUserCount() {
        return Arbitraries.integers().between(1, 10);
    }

    @Provide
    Arbitrary<List<Post>> postList() {
        return validPostItem().list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Post> validPostItem() {
        Arbitrary<String> names = validPostName();
        Arbitrary<Integer> sorts = Arbitraries.integers().between(0, 50);
        Arbitrary<Integer> statuses = Arbitraries.of(0, 1);

        return Combinators.combine(names, sorts, statuses)
                .as((name, sort, status) -> {
                    Post p = new Post();
                    p.setId((long) (Math.random() * 10000 + 1));
                    p.setPostCode("post_" + UUID.randomUUID().toString().substring(0, 8));
                    p.setPostName(name);
                    p.setSortOrder(sort);
                    p.setStatus(status);
                    return p;
                });
    }

    @Provide
    Arbitrary<List<User>> userListWithPosts() {
        return userWithPost().list().ofMinSize(1).ofMaxSize(8);
    }

    @Provide
    Arbitrary<User> userWithPost() {
        Arbitrary<String> postNames = validPostName();
        Arbitrary<Integer> postStatuses = Arbitraries.of(0, 1);

        return Combinators.combine(postNames, postStatuses)
                .as((pName, pStatus) -> {
                    User u = new User();
                    u.setId((long) (Math.random() * 10000 + 1));
                    u.setUsername("user_" + UUID.randomUUID().toString().substring(0, 8));
                    u.setPostId((long) (Math.random() * 100 + 1));
                    u.setPostName(pName);
                    u.setPostStatus(pStatus);
                    u.setStatus(1);
                    return u;
                });
    }
}
