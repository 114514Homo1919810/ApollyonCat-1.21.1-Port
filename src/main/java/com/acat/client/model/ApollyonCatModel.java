package com.acat.client.model;

import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 亚小猫模型 —— 使用 Blockbench 自建模型 a_cat.java（用户绘制）
 * ─────────────────────────────────────────────────────────────
 * 网格：原版猫的组结构（head/body/front_legs/back_legs/tail/tail2），
 *       整体尺寸为原版猫的 2 倍，贴图 128x64（在渲染器里以 0,0,0 为中心缩小 2 倍）。
 * 动画：行走 / 奔跑腿部摆动与尾巴摆动逻辑 = 原版 OcelotModel.setupAnim 的
 *       WALK_STATE / SPRINT_STATE 分支（front_left/right_leg ↔ 原版前腿，
 *       back_left/right_leg ↔ 原版后腿），逐行一致。
 * 待命坐姿：仆从版(Summoned)被指挥号角等置为 isStaying() 待命时，逐格复刻
 *       原版 1.20.1 CatModel.prepareMobModel 的 isInSittingPose() 分支。
 *
 * ★ 数值映射依据（如何做到"逐格一致"）：
 *   本模型每个部件的烘焙位与 1.20.1 原版 ocelot 骨架只差一个常量平移
 *   （displayed = 原版坐标 − y12，且部件尺寸 2 倍后由渲染器缩回 0.5）。
 *   因此：坐姿【角度】直接抄原版绝对值；坐姿【枢轴位移增量】= 原版位移 × 2
 *   （渲染器 0.5 缩放会把它还原成与原版相同的画面位移）。
 *   详见 applySitPose() 内每个常量的原版出处注释。
 */
@OnlyIn(Dist.CLIENT)
public class ApollyonCatModel<T extends LivingEntity> extends AgeableListModel<T> {

    // ---- 骨骼 ----
    protected final ModelPart head;
    protected final ModelPart body;
    protected final ModelPart front_left_leg;
    protected final ModelPart front_right_leg;
    protected final ModelPart back_left_leg;
    protected final ModelPart back_right_leg;
    protected final ModelPart tail;
    protected final ModelPart tail2;

    // ==================== ★ 卧蹴渐变量 ====================
    // 原版 1.20.1 CatModel 是在 prepareMobModel(entity, ..., partialTicks) 里用【真正的 partialTicks】
    // 读出 entity.getLieDownAmount(...) 等三个值存为模型字段，setupAnim 再去取这些字段。
    // 绝不能把 setupAnim 的 ageInTicks（= tickCount + 当前 partialTicks，持续增长的大数）当成分数传给 Mth.lerp，
    // 否则会得到巨大抖动值 → 头/腿乱甩、模型错乱。
    private float lieDownAmount;
    private float lieDownAmountTail;
    private float relaxStateOneAmount;

    // ==================== 烘焙默认位（与 createBodyLayer 一致，每帧复位用） ====================
    private static final float HEAD_BAKE_Y = 6.0F;
    private static final float HEAD_BAKE_Z = -18.0F;
    private static final float BODY_BAKE_Y = 0.0F;
    private static final float BODY_BAKE_Z = -20.0F;
    private static final float BODY_BAKE_XROT = (float) (Math.PI / 2.0);
    private static final float FRONT_LEG_BAKE_Y = 4.2F;
    private static final float FRONT_LEG_BAKE_Z = -10.0F;
    private static final float BACK_LEG_BAKE_Y = 12.0F;
    private static final float BACK_LEG_BAKE_Z = 10.0F;
    private static final float TAIL1_BAKE_Y = 7.0F;
    private static final float TAIL1_BAKE_Z = 16.0F;
    private static final float TAIL2_BAKE_Y = 7.0F;
    private static final float TAIL2_BAKE_Z = 32.0F;
    private static final float TAIL_BAKE_XROT = 1.5708F;   // π/2：烘焙时两节尾巴已转平
    private static final float FRONT_LEG_BAKE_X = 2.2F;    // bake front-leg lateral offset (left = +, right = -); lying tweaks source from vanilla
    private static final float BACK_LEG_BAKE_X = 2.2F;     // bake hind-leg lateral offset (left = +, right = -)

    // ==================== ★ 待命坐姿（逐格复刻原版 1.20.1 CatModel 坐姿分支） ====================
    // 原版 CatModel.prepareMobModel，isInSittingPose()==true 时（数值出处见注释）：
    //   body.xRot = π/4；body.y 12→8、z −10→−5；
    //   head.y 15→11.7、z −9→−8；
    //   leftHindLeg/rightHindLeg.xRot = −π/2，y 18→21、z 5→1；
    //   leftFrontLeg/rightFrontLeg.xRot = −0.15707964，y 14.1→16.1、z −5→−7；
    //   tail1.xRot = 1.7278761，y 15→23、z 8→6；tail2.xRot = 2.670354，y 20→22、z 14→13.2。
    // 位移增量按"本模型=原版×2"换算（渲染器 0.5 缩放还原成原版画面位移）。
    private static final float SIT_BODY_XROT = (float) (Math.PI / 4.0);   // 原版 body.xRot = π/4
    private static final float SIT_BODY_DY = -8.0F;                        // 原版 y −4（上移） ×2
    private static final float SIT_BODY_DZ = 10.0F;                        // 原版 z +5（后收） ×2
    private static final float SIT_HEAD_DY = -6.6F;                        // 原版 y −3.3（抬高） ×2
    private static final float SIT_HEAD_DZ = 2.0F;                         // 原版 z +1（后收） ×2
    private static final float SIT_HIND_LEG_XROT = -(float) (Math.PI / 2.0); // 原版 −π/2（前折收腹）
    private static final float SIT_HIND_LEG_DY = 6.0F;                     // 原版 y +3（后腿下压坐地） ×2
    private static final float SIT_HIND_LEG_DZ = -8.0F;                    // 原版 z −4（前折） ×2
    private static final float SIT_FRONT_LEG_XROT = -0.15707964F;          // 原版前腿 xRot
    private static final float SIT_FRONT_LEG_DY = 4.0F;                    // 原版 y +2（撑地下移） ×2
    private static final float SIT_FRONT_LEG_DZ = -4.0F;                   // 原版 z −2（前撑） ×2
    private static final float SIT_TAIL1_XROT = 1.7278761F;                // 原版 tail1.xRot（绕身卷起）
    private static final float SIT_TAIL1_DY = 15.0F;                       // 原版 y +8（尾根落到坐地高度） ×2
    private static final float SIT_TAIL1_DZ = -4.0F;                       // 原版 z −2（前绕） ×2
    private static final float SIT_TAIL2_XROT = 2.670354F;                 // 原版 tail2.xRot（第二节卷回身侧）
    private static final float SIT_TAIL2_DY = 13.0F;                       // 原版 y +2 ×2
    private static final float SIT_TAIL2_DZ = -5.6F;                       // 原版 z −0.8 ×2

    public ApollyonCatModel(ModelPart root) {
        super(true, 10.0F, 4.0F);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.front_left_leg = root.getChild("front_left_leg");
        this.front_right_leg = root.getChild("front_right_leg");
        this.back_left_leg = root.getChild("back_left_leg");
        this.back_right_leg = root.getChild("back_right_leg");
        this.tail = root.getChild("tail");
        this.tail2 = root.getChild("tail2");
    }

    /** Blockbench 导出的网格（128x64 贴图），原样保留 */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48).addBox(-3.0F, -0.04F, -8.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 20).addBox(-4.0F, -6.0F, 0.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 20).addBox(2.0F, -6.0F, 0.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 48).addBox(-6.0687F, -7.686F, -6.789F, 12.0F, 4.0F, 12.0F, new CubeDeformation(-0.2F))
                        .texOffs(56, 44).addBox(-8.9313F, -4.8205F, -9.9265F, 18.0F, 2.0F, 18.0F, new CubeDeformation(-0.45F)),
                PartPose.offset(0.0F, 6.0F, -18.0F));

        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, 6.0F, -16.0F, 8.0F, 32.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -20.0F, 1.5708F, 0.0F, 0.0F));

        CubeListBuilder frontLeg = CubeListBuilder.create().texOffs(80, 0).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 20.0F, 4.0F, new CubeDeformation(0.0F));
        partdefinition.addOrReplaceChild("front_left_leg", frontLeg, PartPose.offset(2.2F, 4.2F, -10.0F));
        partdefinition.addOrReplaceChild("front_right_leg", frontLeg, PartPose.offset(-2.2F, 4.2F, -10.0F));

        CubeListBuilder backLeg = CubeListBuilder.create().texOffs(16, 26).addBox(-2.0F, 0.0F, 2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F));
        partdefinition.addOrReplaceChild("back_left_leg", backLeg, PartPose.offset(2.2F, 12.0F, 10.0F));
        partdefinition.addOrReplaceChild("back_right_leg", backLeg, PartPose.offset(-2.2F, 12.0F, 10.0F));

        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 30).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.0F, 16.0F, 1.5708F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(8, 30).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.0F, 32.0F, 1.5708F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 64);
    }

    @Override
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of(this.head);
    }

    @Override
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList.of(this.body, this.front_left_leg, this.front_right_leg,
                this.back_left_leg, this.back_right_leg, this.tail, this.tail2);
    }

    // ---- 供渲染层（光环等）访问 ----
    public ModelPart head() {
        return this.head;
    }

    public ModelPart tailPart() {
        return this.tail;
    }

    public ModelPart tail2Part() {
        return this.tail2;
    }

    /** 是否处于"待命(坐下)"姿态：仆从版(Summoned)被命令待命(isStaying)时 */
    private boolean isSittingPose(LivingEntity entity) {
        return entity instanceof Summoned summoned && summoned.isStaying() && !entity.isSprinting();
    }

    /**
     * 每帧先把全部动画部件复位到烘焙姿态（等价原版 prepareMobModel 开头的复位，
     * 原版每帧都先复位再叠姿态，这里照做可避免坐姿→站姿切换时残留位移/角度）。
     */
    private void resetPose() {
        this.head.y = HEAD_BAKE_Y;
        this.head.z = HEAD_BAKE_Z;
        this.body.y = BODY_BAKE_Y;
        this.body.z = BODY_BAKE_Z;
        this.body.xRot = BODY_BAKE_XROT;   // 烘焙平躺角 π/2（原版站立时 setupAnim 同样强制 π/2）
        this.front_left_leg.y = FRONT_LEG_BAKE_Y;
        this.front_left_leg.z = FRONT_LEG_BAKE_Z;
        this.front_right_leg.y = FRONT_LEG_BAKE_Y;
        this.front_right_leg.z = FRONT_LEG_BAKE_Z;
        this.back_left_leg.y = BACK_LEG_BAKE_Y;
        this.back_left_leg.z = BACK_LEG_BAKE_Z;
        this.back_right_leg.y = BACK_LEG_BAKE_Y;
        this.back_right_leg.z = BACK_LEG_BAKE_Z;
        this.front_left_leg.x = FRONT_LEG_BAKE_X;
        this.front_right_leg.x = -FRONT_LEG_BAKE_X;
        this.back_left_leg.x = BACK_LEG_BAKE_X;
        this.back_right_leg.x = -BACK_LEG_BAKE_X;
        this.tail.y = TAIL1_BAKE_Y;
        this.tail.z = TAIL1_BAKE_Z;
        this.tail.xRot = TAIL_BAKE_XROT;
        this.tail2.y = TAIL2_BAKE_Y;
        this.tail2.z = TAIL2_BAKE_Z;
        this.tail2.xRot = TAIL_BAKE_XROT;
        // 卧蹴时会改 head/front_right/back_right 的 zRot / xRot，这里每帧归零，避免起身后残留倾斜
        this.head.zRot = 0.0F;
        this.head.xRot = 0.0F;
        this.head.yRot = 0.0F;
        this.head.x = 0.0F;
        this.body.xRot = BODY_BAKE_XROT;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.body.x = 0.0F;
        this.front_left_leg.zRot = 0.0F;
        this.front_right_leg.zRot = 0.0F;
        this.back_left_leg.zRot = 0.0F;
        this.back_right_leg.zRot = 0.0F;
        this.front_left_leg.yRot = 0.0F;
        this.front_right_leg.yRot = 0.0F;
        this.back_left_leg.yRot = 0.0F;
        this.back_right_leg.yRot = 0.0F;
        this.front_left_leg.xRot = 0.0F;   // 卧蹴会强制设置前腿 xRot，起身/走动前先归零
        this.front_right_leg.xRot = 0.0F;
        this.back_left_leg.xRot = 0.0F;
        this.back_right_leg.xRot = 0.0F;
        this.tail.yRot = 0.0F;
        this.tail.zRot = 0.0F;
        this.tail.x = 0.0F;
        this.tail2.yRot = 0.0F;
        this.tail2.zRot = 0.0F;
        this.tail2.x = 0.0F;
    }

    /**
     * 逐格复刻原版 1.20.1 CatModel 的坐姿：
     *   - 角度全部抄原版绝对值（模型坐标系一致，旋转直接等价）；
     *   - 枢轴位移按"本模型=原版×2"换算（渲染器缩回 0.5 后画面位移与原版一致）；
     *   - 头不再加任何原版没有的呼吸/点头摆动，抬头角度由实体注视(headPitch)决定，
     *     与原版坐姿一致（CatModel 坐姿时头部只保留注视俯仰/偏航）。
     */
    private void applySitPose() {
        // 躯干：整体后收并上移，倾角从水平(π/2)压到 π/4 —— 头胸端抬起、臀部下沉
        this.body.xRot = SIT_BODY_XROT;
        this.body.y += SIT_BODY_DY;
        this.body.z += SIT_BODY_DZ;
        // 头：抬高并略后收（保持注视方向不变）
        this.head.y += SIT_HEAD_DY;
        this.head.z += SIT_HEAD_DZ;
        // 后腿：向前折 90° 收于腹下，并随躯干下沉坐地
        this.back_left_leg.xRot = SIT_HIND_LEG_XROT;
        this.back_left_leg.y += SIT_HIND_LEG_DY;
        this.back_left_leg.z += SIT_HIND_LEG_DZ;
        this.back_right_leg.xRot = SIT_HIND_LEG_XROT;
        this.back_right_leg.y += SIT_HIND_LEG_DY;
        this.back_right_leg.z += SIT_HIND_LEG_DZ;
        // 前腿：保持竖直并微前倾撑地
        this.front_left_leg.xRot = SIT_FRONT_LEG_XROT;
        this.front_left_leg.y += SIT_FRONT_LEG_DY;
        this.front_left_leg.z += SIT_FRONT_LEG_DZ;
        this.front_right_leg.xRot = SIT_FRONT_LEG_XROT;
        this.front_right_leg.y += SIT_FRONT_LEG_DY;
        this.front_right_leg.z += SIT_FRONT_LEG_DZ;
        // 尾巴两节：尾根落到坐地高度并绕身卷起（第二节卷回身侧）
        this.tail.xRot = SIT_TAIL1_XROT;
        this.tail.y += SIT_TAIL1_DY;
        this.tail.z += SIT_TAIL1_DZ;
        this.tail2.xRot = SIT_TAIL2_XROT;
        this.tail2.y += SIT_TAIL2_DY;
        this.tail2.z += SIT_TAIL2_DZ;
    }

    /**
     * ★ 睡觉（卧蹴）姿势：现在与待机坐下动画完全一致（用户要求）。
     *   当猫进入卧蹴(lieDownAmount>0)状态时，直接显示坐姿。
     *   渐变量必须在 prepareMobModel 里读入：与 1.20.1 原版 CatModel 完全一致。
     *   这里传入的 partialTicks 才是真正的每帧插值分数(0..1)。
     *   读出 entity.getLieDownAmount(partialTicks) 等三个值存为模型字段，setupAnim 再去取字段。
     *   不要把 setupAnim 的 ageInTicks(tickCount+partialTicks) 当成分数传入，否则 Mth.lerp 会得到巨大抖动值→模型错乱。
     */
    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        this.lieDownAmount = 0.0F;
        this.lieDownAmountTail = 0.0F;
        this.relaxStateOneAmount = 0.0F;
        if (entity instanceof com.acat.entity.ApollyonCatServantEntity cat) {
            this.lieDownAmount = cat.getLieDownAmount(partialTicks);
            this.lieDownAmountTail = cat.getLieDownAmountTail(partialTicks);
            this.relaxStateOneAmount = cat.getRelaxStateOneAmount(partialTicks);
        }
    }

    /**
     * 原版 OcelotModel.setupAnim + CatModel 坐姿分支：
     *   - 坐姿(state==3)：只保留头部注视，不跑腿部/尾巴摆动；
     *   - 奔跑/行走：逐行抄原版 WALK_STATE / SPRINT_STATE 的腿部与尾尖摆动公式。
     */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.resetPose();
        // 头部注视与原版 OcelotModel.setupAnim 一致：总是先按注视设置 head 俯仰/偏航；
        // 若处于卧蹴(lieDownAmount>0)，其后的 CatModel 睡眠分支会再用 rotlerpRad 覆盖 head 角度。
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);

        if (this.isSittingPose(entity)) {
            this.applySitPose();
            return;
        }

        if (entity.isSprinting()) {
            // 奔跑：前腿相位偏移（原版 SPRINT_STATE）
            this.back_left_leg.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
            this.back_right_leg.xRot = Mth.cos(limbSwing * 0.6662F + 0.3F) * limbSwingAmount;
            this.front_left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI + 0.3F) * limbSwingAmount;
            this.front_right_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
            this.tail2.xRot = 1.7278761F + ((float) Math.PI / 10F) * Mth.cos(limbSwing) * limbSwingAmount;
        } else {
            // 行走 / 站立：原版 WALK_STATE（站立时 limbSwingAmount≈0，四腿垂直，完全复刻原版待机站姿）
            this.back_left_leg.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
            this.back_right_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
            this.front_left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
            this.front_right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
            this.tail2.xRot = 1.7278761F + ((float) Math.PI / 4F) * Mth.cos(limbSwing) * limbSwingAmount;
        }

        // ★ 睡觉（卧蹴）：与待机坐下动画完全一致 —— 用户要求，直接显示坐姿。
        // 渐变量已在 prepareMobModel 用真正 partialTicks 读好存为字段（不能用这里的 ageInTicks）。
        if (this.lieDownAmount > 0.0F) {
            this.applySitPose();
        }
    }

}
